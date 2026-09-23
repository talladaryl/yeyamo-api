package com.yeyamo_mobile.api.event_service.event;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.event_service.models.Event;
import com.yeyamo_mobile.api.event_service.outbox.EventOutboxMessage;
import com.yeyamo_mobile.api.event_service.outbox.EventOutboxRepository;

@Component
public class EventPublisher {

    private final ObjectMapper objectMapper;
    private final EventOutboxRepository repository;

    public EventPublisher(
            ObjectMapper objectMapper,
            EventOutboxRepository repository
    ) {
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    public void publishCreated(Event event, String correlationId, String actorId) {
        publish("event.created", event, correlationId, actorId);
    }

    public void publishUpdated(Event event, String correlationId, String actorId) {
        publish("event.updated", event, correlationId, actorId);
    }

    /**
     * Explicit business transition consumed by content-service. This is never
     * emitted from Event creation while the Event remains pending.
     */
    public void publishPublished(Event event, String correlationId, String actorId) {
        publish("event.published", event, correlationId, actorId);
    }

    public void publishCancelled(Event event, String correlationId, String actorId) {
        publish("event.cancelled", event, correlationId, actorId);
    }

    public void publishCompleted(Event event, String correlationId, String actorId) {
        publish("event.completed", event, correlationId, actorId);
    }

    /**
     * Emits a notification-only event with explicit recipients.  The event
     * service selects participants from its own transactional read model; the
     * notification service never has to guess or fan out to every user.
     */
    public void publishTargeted(String eventType, Event event, Collection<String> recipients,
            String correlationId, String actorId, Map<String, Object> details) {
        List<String> recipientIds = recipients == null ? List.of() : recipients.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        if (recipientIds.isEmpty()) return;
        Map<String, Object> payload = new LinkedHashMap<>();
        DomainEventPayload base = toPayload(event);
        payload.put("eventId", base.eventId());
        payload.put("placeId", base.placeId());
        payload.put("organizerUserId", base.organizerUserId());
        payload.put("title", base.title());
        payload.put("startAt", base.startAt());
        payload.put("endAt", base.endAt());
        payload.put("status", base.status());
        payload.put("visibility", base.visibility());
        payload.put("recipientIds", recipientIds);
        if (recipientIds.size() == 1) payload.put("recipientId", recipientIds.getFirst());
        if (details != null) payload.putAll(details);

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", UUID.randomUUID());
        envelope.put("eventType", eventType);
        envelope.put("eventVersion", 1);
        envelope.put("occurredAt", Instant.now());
        envelope.put("producer", "event-service");
        envelope.put("aggregateType", "event");
        envelope.put("aggregateId", event.getId().toString());
        envelope.put("correlationId", correlationId);
        envelope.put("actorId", actorId);
        envelope.put("payload", payload);
        try {
            EventOutboxMessage message = new EventOutboxMessage();
            message.setId((UUID) envelope.get("eventId"));
            message.setAggregateId(event.getId().toString());
            message.setEventType(eventType);
            message.setPayload(objectMapper.writeValueAsString(envelope));
            message.setOccurredAt((Instant) envelope.get("occurredAt"));
            repository.save(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Impossible de serialiser l'evenement cible " + event.getId(), exception);
        }
    }

    private void publish(String eventType, Event event, String correlationId, String actorId) {
        DomainEvent domainEvent = DomainEvent.of(eventType, toPayload(event), correlationId, actorId);
        try {
            EventOutboxMessage message = new EventOutboxMessage();
            message.setId(domainEvent.eventId());
            message.setAggregateId(event.getId().toString());
            message.setEventType(domainEvent.eventType());
            message.setPayload(objectMapper.writeValueAsString(domainEvent));
            message.setOccurredAt(domainEvent.occurredAt());
            repository.save(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Impossible de serialiser l'evenement " + event.getId(), exception);
        }
    }

    private DomainEventPayload toPayload(Event event) {
        return new DomainEventPayload(
                event.getId(),
                event.getPlaceId(),
                event.getOwnerUserId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartAt(),
                event.getEndAt(),
                event.getStatus(),
                event.getVisibility(),
                event.getCoverMediaId(),
                event.isPublishToFeed(),
                event.isPublishToStory(),
                event.getGeography() == null ? null : event.getGeography().getCountryCode(),
                event.getGeography() == null ? null : event.getGeography().getLanguageCode(),
                event.getLocationName(),
                event.getGeography() == null ? null : event.getGeography().getLatitude(),
                event.getGeography() == null ? null : event.getGeography().getLongitude()
        );
    }
}
