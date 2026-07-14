package com.yeyamo_mobile.api.catalog_service.infrastructure.outbox;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.catalog_service.application.port.CatalogOutboxPort;
import com.yeyamo_mobile.api.catalog_service.domain.model.CatalogAsset;

@Component
public class JpaCatalogOutboxAdapter implements CatalogOutboxPort {
    private final CatalogOutboxRepository repository;
    private final ObjectMapper mapper;
    public JpaCatalogOutboxAdapter(CatalogOutboxRepository repository, ObjectMapper mapper) {
        this.repository = repository; this.mapper = mapper;
    }
    @Override public void append(String eventType, CatalogAsset asset, String correlationId, String actorId) {
        UUID eventId = UUID.randomUUID();
        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("assetId", asset.getId()); payload.put("type", asset.getType());
        payload.put("ownerId", asset.getOwnerId()); payload.put("name", asset.getName());
        payload.put("slug", asset.getSlug()); payload.put("categoryCode", asset.getCategoryCode());
        payload.put("regionCode", asset.getRegionCode()); payload.put("city", asset.getCity());
        payload.put("latitude", asset.getLocation().latitude());
        payload.put("longitude", asset.getLocation().longitude()); payload.put("status", asset.getStatus());
        Map<String,Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId); envelope.put("eventType", eventType);
        envelope.put("eventVersion", 1); envelope.put("occurredAt", Instant.now());
        envelope.put("producer", "catalog-service"); envelope.put("aggregateType", "catalog-asset");
        envelope.put("aggregateId", asset.getId().toString());
        envelope.put("correlationId", normalize(correlationId, eventId.toString()));
        envelope.put("actorId", normalize(actorId, "system")); envelope.put("payload", payload);
        CatalogOutboxEvent event = new CatalogOutboxEvent();
        event.setId(eventId); event.setAggregateId(asset.getId().toString());
        event.setEventType(eventType); event.setOccurredAt(Instant.now());
        try { event.setPayload(mapper.writeValueAsString(envelope)); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize catalog event", ex); }
        repository.save(event);
    }
    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
