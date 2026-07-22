package com.yeyamo_mobile.api.catalog_service.infrastructure.outbox;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.catalog_service.application.port.OutboxPort;

@Component
public class JpaCatalogOutboxAdapter implements OutboxPort {
    private final CatalogOutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public JpaCatalogOutboxAdapter(CatalogOutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(String eventType, String aggregateId, String actorId, String correlationId,
            Map<String, String> payload) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", eventType);
        envelope.put("eventVersion", 1);
        envelope.put("occurredAt", occurredAt);
        envelope.put("producer", "catalog-service");
        envelope.put("correlationId", correlationId);
        envelope.put("actorId", actorId);
        envelope.put("payload", payload);
        try {
            CatalogOutboxEventEntity event = new CatalogOutboxEventEntity();
            event.setId(eventId); 
            event.setAggregateType("CatalogCollection");
            event.setAggregateId(aggregateId);
            event.setEventType(eventType); 
            event.setPayload(objectMapper.writeValueAsString(envelope));
            event.setOccurredAt(occurredAt);
            repository.save(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize catalog domain event", exception);
        }
    }
}
