package com.yeyamo_mobile.api.catalog_service.infrastructure.messaging;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.catalog_service.application.CatalogAssetService;
import com.yeyamo_mobile.api.catalog_service.domain.model.AssetStatus;
import com.yeyamo_mobile.api.catalog_service.domain.model.AssetType;

@Component
public class IngestionEventConsumer {
    private final ObjectMapper mapper;
    private final CatalogAssetService service;
    private final ProcessedEventRepository processed;
    public IngestionEventConsumer(ObjectMapper mapper, CatalogAssetService service, ProcessedEventRepository processed) {
        this.mapper = mapper; this.service = service; this.processed = processed;
    }

    @KafkaListener(topics = "${yeyamo.kafka.topics.catalog-ingestion-events:catalog.ingestion.events}",
            groupId = "${spring.kafka.consumer.group-id:catalog-service}")
    @Transactional
    public void consume(String raw) throws Exception {
        JsonNode envelope = mapper.readTree(raw);
        UUID eventId = UUID.fromString(required(envelope, "eventId"));
        if (processed.existsById(eventId)) return;
        if (!"catalog.asset.ingested".equals(required(envelope, "eventType"))
                || envelope.path("eventVersion").asInt(0) != 1) {
            throw new IllegalArgumentException("Unsupported ingestion event contract");
        }
        JsonNode payload = envelope.path("payload");
        String source = required(payload, "source");
        String externalId = required(payload, "externalId");
        service.synchronizeExternalAsset(source, externalId, type(required(payload, "assetType")), null,
                required(payload, "name"), null, text(payload, "description"), text(payload, "categoryCode"),
                text(payload, "regionCode"), text(payload, "city"), text(payload, "district"),
                text(payload, "address"), requiredDouble(payload, "latitude"), requiredDouble(payload, "longitude"),
                AssetStatus.DRAFT, text(envelope, "correlationId"), "ingestion-service");
        ProcessedEventEntity receipt = new ProcessedEventEntity();
        receipt.setEventId(eventId); receipt.setEventType("catalog.asset.ingested"); receipt.setProcessedAt(Instant.now());
        processed.save(receipt);
    }

    private AssetType type(String raw) {
        try { return AssetType.valueOf(raw.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Unsupported assetType: " + raw); }
    }
    private String required(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }
    private double requiredDouble(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isNumber()) throw new IllegalArgumentException(field + " is required");
        return value.asDouble();
    }
    private String text(JsonNode node, String field) { JsonNode value = node.get(field); return value == null || value.isNull() ? null : value.asText(); }
}
