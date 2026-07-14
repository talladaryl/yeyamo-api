package com.yeyamo_mobile.api.user_service.infrastructure.outbox;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.user_service.application.port.OutboxPort;

@Component
public class JpaOutboxAdapter implements OutboxPort {
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public JpaOutboxAdapter(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(String eventType, UUID profileId, String actorId, String correlationId,
            Map<String, Object> payload) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", eventType);
        envelope.put("eventVersion", 1);
        envelope.put("occurredAt", occurredAt);
        envelope.put("producer", "user-service");
        envelope.put("correlationId", correlationId);
        envelope.put("actorId", actorId);
        envelope.put("payload", payload);
        try {
            OutboxEventEntity event = new OutboxEventEntity();
            event.setId(eventId); event.setAggregateType("UserProfile"); event.setAggregateId(profileId.toString());
            event.setEventType(eventType); event.setPayload(objectMapper.writeValueAsString(envelope));
            event.setOccurredAt(occurredAt);
            repository.save(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize user domain event", exception);
        }
    }
}
