package com.yeyamo_mobile.api.event_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.event_service.models.Event;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public EventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${yeyamo.kafka.topics.event-events:event.events}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
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
            String payload = objectMapper.writeValueAsString(domainEvent);
            kafkaTemplate.send(topic, event.getId().toString(), payload);
        } catch (JsonProcessingException exception) {
            log.error("Impossible de serialiser l'evenement Kafka pour l'evenement {}", event.getId(), exception);
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
