package com.yeyamo_mobile.api.catalog_service.infrastructure.outbox;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.catalog_service.application.port.CatalogOutboxPort;
import com.yeyamo_mobile.api.catalog_service.application.port.OutboxPort;
import com.yeyamo_mobile.api.catalog_service.domain.model.CatalogAsset;

@Component
public class JpaCatalogOutboxAdapter implements OutboxPort, CatalogOutboxPort {
    private final CatalogOutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public JpaCatalogOutboxAdapter(CatalogOutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(String eventType, String aggregateId, String actorId, String correlationId,
            Map<String, String> payload) {
        appendEnvelope(eventType, "CatalogCollection", aggregateId, actorId, correlationId, payload);
    }

    @Override
    public void append(String eventType, CatalogAsset asset, String correlationId, String actorId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("assetId", asset.getId());
        payload.put("type", asset.getType());
        payload.put("ownerId", asset.getOwnerId());
        payload.put("source", asset.getSource());
        payload.put("externalId", asset.getExternalId());
        payload.put("name", asset.getName());
        payload.put("slug", asset.getSlug());
        payload.put("categoryCode", asset.getCategoryCode());
        payload.put("regionCode", asset.getRegionCode());
        payload.put("status", asset.getStatus());
        appendEnvelope(eventType, "CatalogAsset", asset.getId().toString(), actorId, correlationId, payload);
    }

    private void appendEnvelope(String eventType, String aggregateType, String aggregateId, String actorId,
            String correlationId, Map<String, ?> payload) {
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
            event.setAggregateType(aggregateType);
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
