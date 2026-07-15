package com.yeyamo_mobile.api.event_service.event;

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

    public void publishCancelled(Event event, String correlationId, String actorId) {
        publish("event.cancelled", event, correlationId, actorId);
    }

    public void publishCompleted(Event event, String correlationId, String actorId) {
        publish("event.completed", event, correlationId, actorId);
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
                event.getTitle(),
                event.getStartAt(),
                event.getStatus()
        );
    }
}
