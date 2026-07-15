package com.yeyamo_mobile.api.event_service.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.event_service.dto.EventRequest;
import com.yeyamo_mobile.api.event_service.dto.EventResponse;
import com.yeyamo_mobile.api.event_service.dto.EventStatusRequest;
import com.yeyamo_mobile.api.event_service.dto.EventSummaryResponse;
import com.yeyamo_mobile.api.event_service.dto.EventUpdateRequest;
import com.yeyamo_mobile.api.event_service.enums.EventStatus;
import com.yeyamo_mobile.api.event_service.enums.RegistrationStatus;
import com.yeyamo_mobile.api.event_service.event.EventPublisher;
import com.yeyamo_mobile.api.event_service.exception.ApiException;
import com.yeyamo_mobile.api.event_service.models.Event;
import com.yeyamo_mobile.api.event_service.models.EventRegistration;
import com.yeyamo_mobile.api.event_service.repository.EventRegistrationRepository;
import com.yeyamo_mobile.api.event_service.repository.EventRepository;

@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EventPublisher eventPublisher;

    public EventService(
            EventRepository eventRepository,
            EventRegistrationRepository registrationRepository,
            EventPublisher eventPublisher
    ) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public EventResponse getById(UUID id) {
        Event event = findEventOrThrow(id);
        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findByPlaceId(UUID placeId, boolean publishedOnly) {
        List<Event> events = publishedOnly
                ? eventRepository.findByPlaceIdAndStatusOrderByStartAtAsc(placeId, EventStatus.PUBLISHED)
                : eventRepository.findByPlaceIdOrderByStartAtAsc(placeId);
        return events.stream().map(EventSummaryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findUpcoming() {
        return eventRepository
                .findByStatusAndStartAtAfterOrderByStartAtAsc(EventStatus.PUBLISHED, Instant.now())
                .stream()
                .map(EventSummaryResponse::from)
                .toList();
    }

    public EventResponse create(EventRequest request, String correlationId, String actorId) {
        validateDates(request.getStartAt(), request.getEndAt());
        validateCapacity(request.getCapacity(), 0);

        Event event = new Event();
        event.setPlaceId(request.getPlaceId());
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setStartAt(request.getStartAt());
        event.setEndAt(request.getEndAt());
        event.setCapacity(request.getCapacity());
        event.setStatus(request.getStatus() != null ? request.getStatus() : EventStatus.PENDING);
        event.setRegisteredCount(0);

        Event saved = eventRepository.save(event);
        eventPublisher.publishCreated(saved, correlationId, actorId);
        return EventResponse.from(saved);
    }

    public EventResponse update(UUID id, EventUpdateRequest request, String correlationId, String actorId) {
        Event event = findEventOrThrow(id);
        ensureModifiable(event);

        validateDates(request.getStartAt(), request.getEndAt());
        validateCapacity(request.getCapacity(), event.getRegisteredCount());

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setStartAt(request.getStartAt());
        event.setEndAt(request.getEndAt());
        event.setCapacity(request.getCapacity());

        Event saved = eventRepository.save(event);
        eventPublisher.publishUpdated(saved, correlationId, actorId);
        return EventResponse.from(saved);
    }

    public EventResponse updateStatus(UUID id, EventStatusRequest request, String correlationId, String actorId) {
        Event event = findEventOrThrow(id);
        EventStatus currentStatus = event.getStatus();
        EventStatus newStatus = request.getStatus();

        if (currentStatus == newStatus) {
            return EventResponse.from(event);
        }

        validateStatusTransition(currentStatus, newStatus);

        event.setStatus(newStatus);
        Event saved = eventRepository.save(event);

        switch (newStatus) {
            case PUBLISHED -> eventPublisher.publishUpdated(saved, correlationId, actorId);
            case CANCELLED -> eventPublisher.publishCancelled(saved, correlationId, actorId);
            case COMPLETED -> eventPublisher.publishCompleted(saved, correlationId, actorId);
            default -> eventPublisher.publishUpdated(saved, correlationId, actorId);
        }

        return EventResponse.from(saved);
    }

    public EventResponse register(UUID eventId, UUID userId) {
        Event event = findEventForUpdateOrThrow(eventId);
        ensureRegisterable(event);

        EventRegistration registration = registrationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseGet(() -> {
                    EventRegistration created = new EventRegistration();
                    created.setEvent(event);
                    created.setUserId(userId);
                    return created;
                });

        if (registration.getStatus() == RegistrationStatus.CONFIRMED) {
            throw new ApiException("ALREADY_REGISTERED", "Utilisateur deja inscrit a cet evenement", HttpStatus.CONFLICT);
        }

        registration.setStatus(RegistrationStatus.CONFIRMED);
        registrationRepository.save(registration);

        event.setRegisteredCount(event.getRegisteredCount() + 1);
        return EventResponse.from(eventRepository.save(event));
    }

    public EventResponse unregister(UUID eventId, UUID userId) {
        Event event = findEventForUpdateOrThrow(eventId);

        EventRegistration registration = registrationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new ApiException(
                        "REGISTRATION_NOT_FOUND",
                        "Inscription introuvable pour cet utilisateur",
                        HttpStatus.NOT_FOUND
                ));

        if (registration.getStatus() != RegistrationStatus.CONFIRMED) {
            throw new ApiException("REGISTRATION_NOT_ACTIVE", "Aucune inscription active pour cet utilisateur", HttpStatus.BAD_REQUEST);
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);

        event.setRegisteredCount(Math.max(0, event.getRegisteredCount() - 1));
        return EventResponse.from(eventRepository.save(event));
    }

    private Event findEventOrThrow(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ApiException("EVENT_NOT_FOUND", "Evenement introuvable", HttpStatus.NOT_FOUND));
    }

    private Event findEventForUpdateOrThrow(UUID id) {
        return eventRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException("EVENT_NOT_FOUND", "Evenement introuvable", HttpStatus.NOT_FOUND));
    }

    private void validateDates(Instant startAt, Instant endAt) {
        if (!endAt.isAfter(startAt)) {
            throw new ApiException("INVALID_DATES", "La date de fin doit etre posterieure a la date de debut", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateCapacity(int capacity, int registeredCount) {
        if (capacity < registeredCount) {
            throw new ApiException(
                    "CAPACITY_TOO_LOW",
                    "La capacite ne peut pas etre inferieure au nombre d'inscrits",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void ensureModifiable(Event event) {
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED) {
            throw new ApiException(
                    "EVENT_NOT_MODIFIABLE",
                    "Un evenement annule ou termine ne peut pas etre modifie",
                    HttpStatus.CONFLICT
            );
        }
    }

    private void ensureRegisterable(Event event) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ApiException("EVENT_NOT_PUBLISHED", "Seuls les evenements publies acceptent des inscriptions", HttpStatus.CONFLICT);
        }
        if (!event.getStartAt().isAfter(Instant.now())) {
            throw new ApiException("EVENT_ALREADY_STARTED", "Impossible de s'inscrire a un evenement deja commence", HttpStatus.CONFLICT);
        }
        if (event.getRegisteredCount() >= event.getCapacity()) {
            throw new ApiException("EVENT_FULL", "La capacite maximale de l'evenement est atteinte", HttpStatus.CONFLICT);
        }
    }

    private void validateStatusTransition(EventStatus current, EventStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == EventStatus.PUBLISHED || next == EventStatus.CANCELLED;
            case PUBLISHED -> next == EventStatus.CANCELLED || next == EventStatus.COMPLETED;
            case CANCELLED, COMPLETED -> false;
        };

        if (!valid) {
            throw new ApiException(
                    "INVALID_STATUS_TRANSITION",
                    "Transition de statut non autorisee de " + current + " vers " + next,
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
