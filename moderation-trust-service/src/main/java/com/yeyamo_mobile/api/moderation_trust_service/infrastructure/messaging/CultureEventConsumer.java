package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.moderation_trust_service.application.CulturalModerationService;
import com.yeyamo_mobile.api.moderation_trust_service.application.ModerationService;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumes Kafka events from culture-service and artisan-service.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Auto-flag content that is marked SACRED or COMMUNITY_RESTRICTED at source.</li>
 *   <li>Record external events in the audit trail.</li>
 *   <li>Dedup via {@link ProcessedEventRepository}.</li>
 * </ul>
 * </p>
 *
 * <p>Events consumed:
 * <ul>
 *   <li>{@code culture.events} — CultureContentPublished / CultureContentUpdated</li>
 *   <li>{@code artisan.events} — ArtworkPublished / ArtworkUpdated / ArtisanVerified</li>
 * </ul>
 * </p>
 */
@Component
public class CultureEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CultureEventConsumer.class);

    private final ObjectMapper mapper;
    private final CulturalModerationService culturalService;
    private final ModerationService moderationService;
    private final ProcessedEventRepository processed;

    public CultureEventConsumer(ObjectMapper mapper,
                                 CulturalModerationService culturalService,
                                 ModerationService moderationService,
                                 ProcessedEventRepository processed) {
        this.mapper           = mapper;
        this.culturalService  = culturalService;
        this.moderationService = moderationService;
        this.processed        = processed;
    }

    @KafkaListener(
            topics  = "${yeyamo.kafka.topics.culture-events:culture.events}",
            groupId = "${spring.kafka.consumer.group-id:moderation-trust-service}")
    @Transactional
    public void culture(String raw) throws Exception {
        JsonNode event = mapper.readTree(raw);
        UUID    id     = uuid(event, "eventId");
        if (processed.existsById(id)) return;

        String type = required(event, "eventType");
        if (event.path("eventVersion").asInt(0) != 1)
            throw new IllegalArgumentException("Unsupported culture event version: " + type);

        JsonNode p = event.path("payload");
        switch (type) {
            case "CultureContentPublished", "CultureContentUpdated" ->
                    handleCultureContent(p, type);
            default ->
                    log.debug("Ignored culture event type: {}", type);
        }

        moderationService.recordExternal(id.toString(), type,
                text(p, "contentId", "unknown"), text(event, "actorId", "system"),
                text(event, "correlationId", id.toString()), "culture-service");
        receipt(id, type);
    }

    @KafkaListener(
            topics  = "${yeyamo.kafka.topics.artisan-events:artisan.events}",
            groupId = "${spring.kafka.consumer.group-id:moderation-trust-service}")
    @Transactional
    public void artisan(String raw) throws Exception {
        JsonNode event = mapper.readTree(raw);
        UUID    id     = uuid(event, "eventId");
        if (processed.existsById(id)) return;

        String type = required(event, "eventType");
        if (event.path("eventVersion").asInt(0) != 1)
            throw new IllegalArgumentException("Unsupported artisan event version: " + type);

        JsonNode p = event.path("payload");
        switch (type) {
            case "ArtworkPublished", "ArtworkUpdated" -> handleArtwork(p, type);
            default -> log.debug("Ignored artisan event type: {}", type);
        }

        moderationService.recordExternal(id.toString(), type,
                text(p, "artworkId", text(p, "artisanId", "unknown")),
                text(event, "actorId", "system"),
                text(event, "correlationId", id.toString()), "artisan-service");
        receipt(id, type);
    }

    // -------------------------------------------------------------------------
    // Handlers
    // -------------------------------------------------------------------------

    /**
     * If a CultureContent is published with a sensitive category,
     * auto-flag it as PENDING (human admin still needs to approve).
     */
    private void handleCultureContent(JsonNode p, String eventType) {
        if (!"CultureContentPublished".equals(eventType)) return;

        String contentId   = text(p, "contentId", null);
        String contentType = text(p, "contentType", "");

        if (contentId == null) return;

        // Auto-flag if source explicitly marks as sacred/restricted
        boolean isSacred      = "SACRED".equalsIgnoreCase(contentType);
        boolean isRestricted  = "COMMUNITY_RESTRICTED".equalsIgnoreCase(text(p, "visibility", ""));

        if (isSacred || isRestricted) {
            SensitiveContentFlag.FlagType flagType = isSacred
                    ? SensitiveContentFlag.FlagType.SACRED_CONTENT
                    : SensitiveContentFlag.FlagType.COMMUNITY_RESTRICTED;

            culturalService.flagSensitiveContent(
                    TargetType.CULTURE_CONTENT, contentId,
                    flagType,
                    "Auto-flagged from source event: " + contentType,
                    "system:culture-consumer");

            log.info("Auto-flagged culture content {} as {}", contentId, flagType);
        }
    }

    private void handleArtwork(JsonNode p, String eventType) {
        // Future: auto-flag counterfeit artworks when source provides such signals
        log.debug("Artwork event {} received for artwork {}", eventType, text(p, "artworkId", "?"));
    }

    // -------------------------------------------------------------------------
    // Helpers (same pattern as SourceEventConsumer)
    // -------------------------------------------------------------------------

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

    private String required(JsonNode n, String f) {
        String v = text(n, f, null);
        if (v == null || v.isBlank())
            throw new IllegalArgumentException(f + " is required");
        return v;
    }

    private String text(JsonNode n, String f, String fallback) {
        JsonNode v = n.get(f);
        return (v == null || v.isNull()) ? fallback : v.asText();
    }
}
