package com.yeyamo_mobile.api.event_service.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.event_service.dto.EventRequest;
import com.yeyamo_mobile.api.event_service.dto.EventParticipantResponse;
import com.yeyamo_mobile.api.event_service.dto.EventResponse;
import com.yeyamo_mobile.api.event_service.dto.EventStatusRequest;
import com.yeyamo_mobile.api.event_service.dto.EventSummaryResponse;
import com.yeyamo_mobile.api.event_service.dto.EventUpdateRequest;
import com.yeyamo_mobile.api.event_service.dto.EventInvitationResponse;
import com.yeyamo_mobile.api.event_service.enums.EventStatus;
import com.yeyamo_mobile.api.event_service.enums.EventVisibility;
import com.yeyamo_mobile.api.event_service.enums.RegistrationStatus;
import com.yeyamo_mobile.api.event_service.event.EventPublisher;
import com.yeyamo_mobile.api.event_service.exception.ApiException;
import com.yeyamo_mobile.api.event_service.models.Event;
import com.yeyamo_mobile.api.event_service.models.EventRegistration;
import com.yeyamo_mobile.api.event_service.models.EventInvitation;
import com.yeyamo_mobile.api.event_service.repository.EventRegistrationRepository;
import com.yeyamo_mobile.api.event_service.repository.EventRepository;
import com.yeyamo_mobile.api.event_service.repository.EventInvitationRepository;
import com.yeyamo_mobile.api.event_service.repository.PlaceReadModelRepository;
import com.yeyamo_mobile.api.event_service.models.PlaceReadModel;
import com.yeyamo_mobile.shared.country.CountryConfigClient;
import com.yeyamo_mobile.shared.country.CountryConfigClient.CountryFeature;
import com.yeyamo_mobile.shared.geography.GeographicFields;

@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EventPublisher eventPublisher;
    private final CountryConfigClient countries;
    private final PlaceReadModelRepository places;
    private final EventInvitationRepository invitations;

    public EventService(
            EventRepository eventRepository,
            EventRegistrationRepository registrationRepository,
            EventPublisher eventPublisher
    ) {
        this(eventRepository, registrationRepository, eventPublisher, null, null, null);
    }

    public EventService(
            EventRepository eventRepository,
            EventRegistrationRepository registrationRepository,
            EventPublisher eventPublisher,
            CountryConfigClient countries,
            PlaceReadModelRepository places
    ) {
        this(eventRepository, registrationRepository, eventPublisher, countries, places, null);
    }

    @Autowired
    public EventService(
            EventRepository eventRepository,
            EventRegistrationRepository registrationRepository,
            EventPublisher eventPublisher,
            CountryConfigClient countries,
            PlaceReadModelRepository places,
            @Autowired(required = false) EventInvitationRepository invitations
    ) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.eventPublisher = eventPublisher;
        this.countries = countries;
        this.places = places;
        this.invitations = invitations;
    }

    @Transactional(readOnly = true)
    public EventResponse getById(UUID id) {
        Event event = findEventOrThrow(id);
        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public EventResponse getById(UUID id, String actor, boolean privileged) {
        Event event = findEventOrThrow(id);
        if (event.getVisibility() == EventVisibility.PRIVATE && !canAccess(event, actor, privileged)) {
            throw new ApiException("EVENT_PRIVATE", "Cet evenement est prive", HttpStatus.FORBIDDEN);
        }
        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findByPlaceId(UUID placeId, boolean publishedOnly) {
        List<Event> events = publishedOnly
                ? eventRepository.findByPlaceIdAndStatusAndVisibilityOrderByStartAtAsc(placeId, EventStatus.PUBLISHED, EventVisibility.PUBLIC)
                : eventRepository.findByPlaceIdOrderByStartAtAsc(placeId);
        return events.stream().map(EventSummaryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findUpcoming() {
        return eventRepository
                .findByStatusAndVisibilityAndStartAtAfterOrderByStartAtAsc(EventStatus.PUBLISHED, EventVisibility.PUBLIC, Instant.now())
                .stream()
                .map(EventSummaryResponse::from)
                .toList();
    }

    public EventResponse create(EventRequest request, String correlationId, String actorId) {
        validateDates(request.getStartAt(), request.getEndAt());
        validateCapacity(request.getCapacity(), 0);
        GeographicFields geography = geography(request);
        validateCountryFeatures(request, geography);
        if (!request.isVirtual() && request.getPlaceId() == null && request.getLocationName() == null) {
            throw new ApiException("PLACE_REQUIRED", "Un Ã©vÃ©nement physique doit Ãªtre associÃ© Ã  un lieu", HttpStatus.BAD_REQUEST);
        }
        validateLocation(request);
        validatePlace(request);

        Event event = new Event();
        event.setPlaceId(request.getPlaceId());
        event.setOwnerUserId(actorId);
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setLocationName(blankToNull(request.getLocationName()));
        event.setLocationAddress(blankToNull(request.getLocationAddress()));
        event.setLocationLatitude(request.getLocationLatitude());
        event.setLocationLongitude(request.getLocationLongitude());
        event.setVisibility(request.getVisibility() == null ? EventVisibility.PUBLIC : request.getVisibility());
        event.setAllowUninvitedParticipants(request.isAllowUninvitedParticipants());
        event.setCommentsParticipantsOnly(request.isCommentsParticipantsOnly());
        event.setShowParticipants(request.isShowParticipants());
        event.setSharingEnabled(request.isSharingEnabled());
        event.setCoverMediaId(request.getCoverMediaId());
        event.setStartAt(request.getStartAt());
        event.setEndAt(request.getEndAt());
        event.setCapacity(request.getCapacity());
        event.setStatus(EventStatus.PENDING);
        event.setRegisteredCount(0);
        event.setVirtual(request.isVirtual());
        event.setAccessibleCountries(request.getAccessibleCountries() == null ? new java.util.HashSet<>() : new java.util.HashSet<>(request.getAccessibleCountries()));
        event.setGeography(geography);

        Event saved = eventRepository.save(event);
        eventPublisher.publishCreated(saved, correlationId, actorId);
        return EventResponse.from(saved);
    }

    public EventResponse update(UUID id, EventUpdateRequest request, String correlationId, String actorId) {
        Event event = findEventOrThrow(id);
        requireOwner(event, actorId, false);
        ensureModifiable(event);

        validateDates(request.getStartAt(), request.getEndAt());
        validateCapacity(request.getCapacity(), event.getRegisteredCount());

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setCoverMediaId(request.getCoverMediaId());
        event.setStartAt(request.getStartAt());
        event.setEndAt(request.getEndAt());
        event.setCapacity(request.getCapacity());

        Event saved = eventRepository.save(event);
        eventPublisher.publishUpdated(saved, correlationId, actorId);
        return EventResponse.from(saved);
    }

    public EventResponse updateStatus(UUID id, EventStatusRequest request, String correlationId, String actorId) {
        return updateStatus(id, request, correlationId, actorId, true);
    }

    public EventResponse updateStatus(UUID id, EventStatusRequest request, String correlationId, String actorId, boolean privileged) {
        Event event = findEventOrThrow(id);
        requireOwner(event, actorId, privileged);
        EventStatus currentStatus = event.getStatus();
        EventStatus newStatus = request.getStatus();

        if (!privileged && newStatus == EventStatus.PUBLISHED) {
            throw new ApiException("EVENT_PUBLICATION_REVIEW_REQUIRED", "La publication d'une sortie requiert une moderation", HttpStatus.FORBIDDEN);
        }

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

    public EventResponse register(UUID eventId, String userId) {
        Event event = findEventForUpdateOrThrow(eventId);
        ensureRegisterable(event);
        if (!event.isAllowUninvitedParticipants() && (invitations == null || !invitations.existsByEventIdAndUserId(eventId, userId))) {
            throw new ApiException("EVENT_INVITATION_REQUIRED", "Une invitation est requise pour cet evenement", HttpStatus.FORBIDDEN);
        }

        var existingRegistration = registrationRepository.findByEventIdAndUserId(eventId, userId);
        if (existingRegistration.filter(r -> r.getStatus() == RegistrationStatus.CONFIRMED).isPresent()) {
            throw new ApiException("ALREADY_REGISTERED", "Utilisateur deja inscrit a cet evenement", HttpStatus.CONFLICT);
        }
        EventRegistration registration = existingRegistration.orElseGet(() -> {
                    EventRegistration created = new EventRegistration();
                    created.setEvent(event);
                    created.setUserId(userId);
                    return created;
                });

        registration.setStatus(RegistrationStatus.CONFIRMED);
        registrationRepository.save(registration);

        event.setRegisteredCount(event.getRegisteredCount() + 1);
        return EventResponse.from(eventRepository.save(event));
    }

    public EventResponse unregister(UUID eventId, String userId) {
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

    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findRegisteredByUser(String userId, int limit) {
        return registrationRepository.findByUserIdAndStatusOrderByRegisteredAtDesc(
                        userId, RegistrationStatus.CONFIRMED,
                        org.springframework.data.domain.PageRequest.of(0, Math.max(1, Math.min(100, limit))))
                .stream()
                .map(EventRegistration::getEvent)
                .map(EventSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventParticipantResponse> findParticipants(UUID eventId, int limit) {
        findEventOrThrow(eventId);
        return registrationRepository.findByEventIdAndStatusOrderByRegisteredAtAsc(
                        eventId, RegistrationStatus.CONFIRMED,
                        org.springframework.data.domain.PageRequest.of(0, Math.max(1, Math.min(200, limit))))
                .stream()
                .map(EventParticipantResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventParticipantResponse> findParticipants(UUID eventId, int limit, String actor, boolean privileged) {
        Event event = findEventOrThrow(eventId);
        if (!event.isShowParticipants() && !canAccess(event, actor, privileged)) {
            throw new ApiException("EVENT_PARTICIPANTS_PRIVATE", "La liste des participants est privee", HttpStatus.FORBIDDEN);
        }
        if (event.isShowParticipants() && !canAccess(event, actor, privileged)) {
            throw new ApiException("EVENT_PARTICIPANTS_FORBIDDEN", "Acces aux participants refuse", HttpStatus.FORBIDDEN);
        }
        return findParticipants(eventId, limit);
    }

    @Transactional
    public EventInvitationResponse invite(UUID eventId, String inviteeUserId, String actor, boolean privileged) {
        Event event = findEventForUpdateOrThrow(eventId);
        requireOwner(event, actor, privileged);
        if (inviteeUserId.equals(actor)) throw new ApiException("EVENT_INVITATION_INVALID", "L'organisateur ne peut pas etre invite", HttpStatus.BAD_REQUEST);
        if (invitations == null) throw new ApiException("EVENT_INVITATIONS_UNAVAILABLE", "Les invitations ne sont pas disponibles", HttpStatus.SERVICE_UNAVAILABLE);
        EventInvitation invitation = invitations.findByEventIdAndUserId(eventId, inviteeUserId)
                .orElseGet(() -> invitations.save(EventInvitation.create(eventId, inviteeUserId, actor)));
        return EventInvitationResponse.from(invitation);
    }

    @Transactional(readOnly = true)
    public List<EventInvitationResponse> invitations(UUID eventId, String actor, boolean privileged) {
        Event event = findEventOrThrow(eventId);
        requireOwner(event, actor, privileged);
        if (invitations == null) throw new ApiException("EVENT_INVITATIONS_UNAVAILABLE", "Les invitations ne sont pas disponibles", HttpStatus.SERVICE_UNAVAILABLE);
        return invitations.findByEventIdOrderByCreatedAtAsc(eventId).stream().map(EventInvitationResponse::from).toList();
    }

    @Transactional
    public void revokeInvitation(UUID eventId, String inviteeUserId, String actor, boolean privileged) {
        Event event = findEventForUpdateOrThrow(eventId);
        requireOwner(event, actor, privileged);
        if (invitations == null) throw new ApiException("EVENT_INVITATIONS_UNAVAILABLE", "Les invitations ne sont pas disponibles", HttpStatus.SERVICE_UNAVAILABLE);
        invitations.findByEventIdAndUserId(eventId, inviteeUserId).ifPresent(invitations::delete);
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

    private void requireOwner(Event event, String actor, boolean privileged) {
        if (privileged) return;
        if (actor == null || event.getOwnerUserId() == null || !event.getOwnerUserId().equals(actor)) {
            throw new ApiException("EVENT_FORBIDDEN", "Cet evenement ne vous appartient pas", HttpStatus.FORBIDDEN);
        }
    }

    private boolean canAccess(Event event, String actor, boolean privileged) {
        if (event.getVisibility() == EventVisibility.PUBLIC || privileged) return true;
        if (actor == null) return false;
        if (actor.equals(event.getOwnerUserId())) return true;
        if (registrationRepository.existsByEventIdAndUserIdAndStatus(event.getId(), actor, RegistrationStatus.CONFIRMED)) return true;
        return invitations != null && invitations.existsByEventIdAndUserId(event.getId(), actor);
    }

    private void validateStatusTransition(EventStatus current, EventStatus next) {
        boolean valid = switch (current) {
            case DRAFT -> next == EventStatus.PENDING_REVIEW || next == EventStatus.ARCHIVED;
            case PENDING, PENDING_REVIEW -> next == EventStatus.PUBLISHED || next == EventStatus.CANCELLED
                    || next == EventStatus.REJECTED || next == EventStatus.ARCHIVED;
            case PUBLISHED -> next == EventStatus.CANCELLED || next == EventStatus.COMPLETED;
            case SUSPENDED -> next == EventStatus.PUBLISHED || next == EventStatus.ARCHIVED;
            case REJECTED -> next == EventStatus.DRAFT || next == EventStatus.ARCHIVED;
            case ARCHIVED, CANCELLED, COMPLETED -> false;
        };

        if (!valid) {
            throw new ApiException(
                    "INVALID_STATUS_TRANSITION",
                    "Transition de statut non autorisee de " + current + " vers " + next,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private GeographicFields geography(EventRequest request) {
        if (request.getCountryCode() == null || request.getCountryCode().isBlank()) return null;
        GeographicFields geography = new GeographicFields(request.getCountryCode());
        geography.setLocation(request.getAdminLevel1Id(), request.getAdminLevel2Id(), request.getCityId(), request.getLocalityId());
        geography.setCoordinates(request.getLatitude(), request.getLongitude());
        geography.setLanguageCode(request.getLanguageCode());
        return geography;
    }

    private void validateCountryFeatures(EventRequest request, GeographicFields geography) {
        if (geography == null) {
            if (countries != null) throw new ApiException("COUNTRY_REQUIRED", "Un pays est requis pour crÃ©er un Ã©vÃ©nement", HttpStatus.BAD_REQUEST);
            return;
        }
        if (countries == null) return;
        try {
            countries.validateFeature(geography.getCountryCode(), CountryFeature.CONTENT_PUBLISHING);
            countries.validateFeature(geography.getCountryCode(), CountryFeature.EVENT_FEATURE);
            countries.validateCity(geography.getCountryCode(), geography.getCityId());
        } catch (CountryConfigClient.CountryConfigException exception) {
            throw new ApiException("COUNTRY_CONFIGURATION_REJECTED", exception.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private void validatePlace(EventRequest request) {
        if (request.isVirtual() || request.getPlaceId() == null || places == null) {
            return;
        }
        PlaceReadModel place = places.findById(request.getPlaceId())
                .orElseThrow(() -> new ApiException("PLACE_NOT_FOUND", "Lieu introuvable", HttpStatus.NOT_FOUND));
        if (!place.isActive()) {
            throw new ApiException("PLACE_NOT_ACTIVE", "Le lieu selectionne n'est pas actif", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateLocation(EventRequest request) {
        if (request.isVirtual()) return;
        if (request.getPlaceId() != null) {
            if (request.getLocationName() != null || request.getLocationAddress() != null
                    || request.getLocationLatitude() != null || request.getLocationLongitude() != null) {
                throw new ApiException("EVENT_LOCATION_AMBIGUOUS", "Choisissez un lieu canonique ou une localisation libre", HttpStatus.BAD_REQUEST);
            }
            return;
        }
        if (blankToNull(request.getLocationName()) == null || blankToNull(request.getLocationAddress()) == null
                || request.getLocationLatitude() == null || request.getLocationLongitude() == null) {
            throw new ApiException("EVENT_LOCATION_REQUIRED", "Un lieu canonique ou une localisation libre complete est requis", HttpStatus.BAD_REQUEST);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
