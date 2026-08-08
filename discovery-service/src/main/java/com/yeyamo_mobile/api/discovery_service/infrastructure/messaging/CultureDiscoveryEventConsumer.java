package com.yeyamo_mobile.api.discovery_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.discovery_service.application.DiscoveryProjectionService;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryDocument;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Consumes Kafka events from culture-service and artisan-service and
 * projects them into the discovery index.
 *
 * <p>All event types use version 1 of the contract.
 * Unknown events are silently ignored after dedup registration so they
 * don't re-trigger on the next poll cycle.</p>
 *
 * <p>Topics consumed:
 * <ul>
 *   <li>{@code culture.events} — CultureContentPublished / CultureContentUpdated</li>
 *   <li>{@code artisan.events} — ArtworkPublished / ArtworkUpdated /
 *       ArtisanVerified / ArtworkAvailabilityChanged</li>
 * </ul>
 * </p>
 */
@Component
public class CultureDiscoveryEventConsumer {

    private final ObjectMapper mapper;
    private final DiscoveryProjectionService projections;
    private final ProcessedEventRepository processed;

    public CultureDiscoveryEventConsumer(
            ObjectMapper mapper,
            DiscoveryProjectionService projections,
            ProcessedEventRepository processed) {
        this.mapper = mapper;
        this.projections = projections;
        this.processed = processed;
    }

    // -------------------------------------------------------------------------
    // culture.events
    // -------------------------------------------------------------------------

    @KafkaListener(
            topics = "${yeyamo.kafka.topics.culture-events:culture.events}",
            groupId = "${spring.kafka.consumer.group-id:discovery-service}")
    @Transactional
    public void culture(String raw) throws Exception {
        JsonNode event = mapper.readTree(raw);
        UUID eventId = uuid(event, "eventId");
        if (processed.existsById(eventId)) return;

        String eventType = required(event, "eventType");
        if (event.path("eventVersion").asInt(0) != 1)
            throw new IllegalArgumentException("Unsupported culture event version: " + eventType);

        JsonNode p = event.path("payload");
        switch (eventType) {
            case "CultureContentPublished", "CultureContentUpdated" -> upsertCultureContent(p, eventType, event);
            default -> { /* ignore — still mark processed */ }
        }
        receipt(eventId, eventType);
    }

    // -------------------------------------------------------------------------
    // artisan.events
    // -------------------------------------------------------------------------

    @KafkaListener(
            topics = "${yeyamo.kafka.topics.artisan-events:artisan.events}",
            groupId = "${spring.kafka.consumer.group-id:discovery-service}")
    @Transactional
    public void artisan(String raw) throws Exception {
        JsonNode event = mapper.readTree(raw);
        UUID eventId = uuid(event, "eventId");
        if (processed.existsById(eventId)) return;

        String eventType = required(event, "eventType");
        if (event.path("eventVersion").asInt(0) != 1)
            throw new IllegalArgumentException("Unsupported artisan event version: " + eventType);

        JsonNode p = event.path("payload");
        switch (eventType) {
            case "ArtworkPublished", "ArtworkUpdated"   -> upsertArtwork(p, event);
            case "ArtisanVerified"                      -> upsertArtisan(p, event);
            case "ArtworkAvailabilityChanged"           -> updateArtworkAvailability(p);
            default -> { /* ignore */ }
        }
        receipt(eventId, eventType);
    }

    // -------------------------------------------------------------------------
    // Projectors
    // -------------------------------------------------------------------------

    private void upsertCultureContent(JsonNode p, String eventType, JsonNode event) {
        String contentId   = required(p, "contentId");
        String sourceId    = "culture:" + contentId;
        var    current     = projections.find(sourceId).orElse(null);
        boolean active     = !"CultureContentDeleted".equals(eventType)
                             && "PUBLISHED".equals(text(p, "status", "DRAFT"));
        Instant published  = active
                ? (current == null || current.publishedAt() == null ? occurred(event) : current.publishedAt())
                : (current != null ? current.publishedAt() : null);

        DiscoveryDocument doc = new DiscoveryDocument(
                stableId(sourceId),
                sourceId,
                cultureType(text(p, "contentType", "CULTURE")),
                required(p, "title"),
                text(p, "summary", null),
                text(p, "categoryCode", null),
                text(p, "adminLevel1Id", null),
                text(p, "cityName", null),
                number(p, "latitude"),
                number(p, "longitude"),
                text(p, "authorId", null),
                current == null ? 0 : current.trendScore(),
                active,
                published,
                Instant.now(),
                // culture extensions
                text(p, "countryCode", null),
                text(p, "adminLevel1Id", null),
                text(p, "cityId", null),
                text(p, "translatedTitles", null),
                text(p, "languageCodes", null),
                text(p, "community", null),
                text(p, "tags", null),
                null, null,             // materials / techniques — not applicable here
                null,                   // artisanId
                text(p, "verificationStatus", null),
                null,                   // availabilityStatus
                null, null,             // price range
                current == null ? 0 : current.popularitySignal()
        );
        projections.project(doc);
    }

    private void upsertArtwork(JsonNode p, JsonNode event) {
        String artworkId = required(p, "artworkId");
        String sourceId  = "artwork:" + artworkId;
        var    current   = projections.find(sourceId).orElse(null);
        boolean active   = "PUBLISHED".equals(text(p, "status", "DRAFT"));
        Instant published = active
                ? (current == null || current.publishedAt() == null ? occurred(event) : current.publishedAt())
                : (current != null ? current.publishedAt() : null);

        BigDecimal priceMin = decimal(p, "priceMin");
        BigDecimal priceMax = decimal(p, "priceMax");

        DiscoveryDocument doc = new DiscoveryDocument(
                stableId(sourceId),
                sourceId,
                DiscoveryType.ARTWORK,
                required(p, "title"),
                text(p, "description", null),
                text(p, "categoryCode", null),
                text(p, "adminLevel1Id", null),
                text(p, "cityName", null),
                number(p, "latitude"),
                number(p, "longitude"),
                text(p, "artisanId", null),
                current == null ? 0 : current.trendScore(),
                active,
                published,
                Instant.now(),
                // culture extensions
                text(p, "countryCode", null),
                text(p, "adminLevel1Id", null),
                text(p, "cityId", null),
                text(p, "translatedTitles", null),
                text(p, "languageCodes", null),
                text(p, "community", null),
                text(p, "tags", null),
                text(p, "materials", null),
                text(p, "techniques", null),
                text(p, "artisanId", null),
                text(p, "verificationStatus", "PENDING"),
                text(p, "availabilityStatus", "AVAILABLE"),
                priceMin,
                priceMax,
                current == null ? 0 : current.popularitySignal()
        );
        projections.project(doc);
    }

    private void upsertArtisan(JsonNode p, JsonNode event) {
        String artisanId = required(p, "artisanId");
        String sourceId  = "artisan:" + artisanId;
        var    current   = projections.find(sourceId).orElse(null);

        DiscoveryDocument doc = new DiscoveryDocument(
                stableId(sourceId),
                sourceId,
                DiscoveryType.ARTISAN,
                required(p, "displayName"),
                text(p, "bio", null),
                text(p, "categoryCode", null),
                text(p, "adminLevel1Id", null),
                text(p, "cityName", null),
                number(p, "latitude"),
                number(p, "longitude"),
                artisanId,
                current == null ? 0 : current.trendScore(),
                true,
                current == null ? occurred(event) : current.publishedAt(),
                Instant.now(),
                // culture extensions
                text(p, "countryCode", null),
                text(p, "adminLevel1Id", null),
                text(p, "cityId", null),
                null, // translatedTitles
                text(p, "languageCodes", null),
                text(p, "community", null),
                text(p, "tags", null),
                text(p, "specialties", null),  // reuse materials field for artisan specialties
                null,
                artisanId,
                "VERIFIED",   // ArtisanVerified event = verified status
                null,
                null, null,
                current == null ? 0 : current.popularitySignal()
        );
        projections.project(doc);
    }

    /**
     * Update only the availability status of an artwork — no full re-index.
     * We load the current document, patch the field, and upsert.
     */
    private void updateArtworkAvailability(JsonNode p) {
        String artworkId  = required(p, "artworkId");
        String sourceId   = "artwork:" + artworkId;
        String newStatus  = required(p, "availabilityStatus");

        projections.find(sourceId).ifPresent(current -> {
            DiscoveryDocument updated = new DiscoveryDocument(
                    current.id(), current.sourceId(), current.type(),
                    current.title(), current.description(),
                    current.categoryCode(), current.regionCode(), current.city(),
                    current.latitude(), current.longitude(), current.authorId(),
                    current.trendScore(), current.active(),
                    current.publishedAt(), Instant.now(),
                    current.countryCode(), current.adminLevel1Id(), current.cityId(),
                    current.translatedTitlesJson(), current.languageCodes(),
                    current.community(), current.tags(),
                    current.materials(), current.techniques(), current.artisanId(),
                    current.verificationStatus(),
                    newStatus,
                    current.priceMin(), current.priceMax(),
                    current.popularitySignal()
            );
            projections.project(updated);
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private DiscoveryType cultureType(String raw) {
        return switch (raw.toUpperCase(java.util.Locale.ROOT)) {
            case "LANGUAGE"  -> DiscoveryType.LANGUAGE;
            case "TRADITION" -> DiscoveryType.TRADITION;
            default          -> DiscoveryType.CULTURE;
        };
    }

    private UUID uuid(JsonNode n, String field) {
        return UUID.fromString(required(n, field));
    }

    private void receipt(UUID id, String type) {
        ProcessedEventEntity e = new ProcessedEventEntity();
        e.setEventId(id);
        e.setEventType(type);
        e.setProcessedAt(Instant.now());
        processed.save(e);
    }

    private UUID stableId(String sourceId) {
        return UUID.nameUUIDFromBytes(sourceId.getBytes(StandardCharsets.UTF_8));
    }

    private String required(JsonNode n, String f) {
        String v = text(n, f, null);
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " is required");
        return v;
    }

    private String text(JsonNode n, String f, String fallback) {
        JsonNode v = n.get(f);
        return (v == null || v.isNull() || v.asText().isBlank()) ? fallback : v.asText();
    }

    private Double number(JsonNode n, String f) {
        JsonNode v = n.get(f);
        return (v == null || v.isNull() || !v.isNumber()) ? null : v.asDouble();
    }

    private BigDecimal decimal(JsonNode n, String f) {
        JsonNode v = n.get(f);
        return (v == null || v.isNull() || !v.isNumber()) ? null : v.decimalValue();
    }

    private Instant occurred(JsonNode e) {
        try {
            String v = text(e, "occurredAt", null);
            return v == null ? Instant.now() : Instant.parse(v);
        } catch (RuntimeException ex) {
            return Instant.now();
        }
    }
}
