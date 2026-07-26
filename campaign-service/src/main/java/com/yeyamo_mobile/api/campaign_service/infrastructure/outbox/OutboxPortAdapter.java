package com.yeyamo_mobile.api.campaign_service.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.campaign_service.application.port.OutboxPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class OutboxPortAdapter implements OutboxPort {
    
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxPortAdapter(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(String eventType, UUID aggregateId, String actorId, String correlationId, Map<String, Object> payload) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(UUID.randomUUID());
        event.setAggregateType("Campaign");
        event.setAggregateId(aggregateId.toString());
        event.setEventType(eventType);
        event.setOccurredAt(Instant.now());
        event.setAttempts(0);
        
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", event.getId().toString());
        envelope.put("eventType", eventType);
        envelope.put("eventVersion", 1);
        envelope.put("occurredAt", event.getOccurredAt().toString());
        envelope.put("producer", "campaign-service");
        envelope.put("aggregateId", aggregateId.toString());
        envelope.put("actorId", actorId);
        envelope.put("correlationId", correlationId);
        envelope.put("payload", payload);
        
        try {
            event.setPayload(objectMapper.writeValueAsString(envelope));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event payload", e);
        }
        
        repository.save(event);
    }
}
