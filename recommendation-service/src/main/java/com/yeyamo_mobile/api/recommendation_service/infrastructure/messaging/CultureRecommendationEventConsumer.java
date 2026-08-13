package com.yeyamo_mobile.api.recommendation_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.recommendation_service.application.RecommendationProjectionService;
import com.yeyamo_mobile.api.recommendation_service.domain.Candidate;
import com.yeyamo_mobile.api.recommendation_service.domain.CandidateKind;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Consumes culture and artisan events and feeds them into the
 * recommendation candidate pool.
 *
 * <p>Mirrors the structure of {@link RecommendationEventConsumer}.</p>
 *
 * <p>Topics:
 * <ul>
 *   <li>{@code culture.events} — culture content lifecycle</li>
 *   <li>{@code artisan.events} — artwork / artisan lifecycle</li>
 * </ul>
 * </p>
 */
@Component
public class CultureRecommendationEventConsumer {

    private final ObjectMapper mapper;
    private final RecommendationProjectionService service;
    private final ProcessedEventRepository processed;

    public CultureRecommendationEventConsumer(
            ObjectMapper mapper,
            RecommendationProjectionService service,
            ProcessedEventRepository processed) {
        this.mapper = mapper;
        this.service = service;
        this.processed = processed;
    }

    // -------------------------------------------------------------------------
    // culture.events
    // -------------------------------------------------------------------------

    @KafkaListener(
            topics = "${yeyamo.kafka.topics.culture-events:culture.events}",
            groupId = "${spring.kafka.consumer.group-id:recommendation-service}")
    @Transactional
    public void culture(String raw) throws Exception {
        handle(raw, "culture");
    }

    // -------------------------------------------------------------------------
    // artisan.events
    // -------------------------------------------------------------------------

    @KafkaListener(
            topics = "${yeyamo.kafka.topics.artisan-events:artisan.events}",
            groupId = "${spring.kafka.consumer.group-id:recommendation-service}")
    @Transactional
    public void artisan(String raw) throws Exception {
        handle(raw, "artisan");
    }

    // -------------------------------------------------------------------------
    // Dispatch
    // -------------------------------------------------------------------------

    private void handle(String raw, String source) throws Exception {
        JsonNode e = mapper.readTree(raw);
        UUID id = UUID.fromString(required(e, "eventId"));
        if (processed.existsById(id)) return;
        if (e.path("eventVersion").asInt(0) != 1)
            throw new IllegalArgumentException("Unsupported event version");

        String type = required(e, "eventType");
        JsonNode p = e.path("payload");
        switch (source) {
            case "culture" -> cultureEvent(type, p);
            case "artisan" -> artisanEvent(type, p);
        }
        processed.save(new ProcessedEventEntity(id, type));
    }

    // -------------------------------------------------------------------------
    // Culture content
    // -------------------------------------------------------------------------

    private void cultureEvent(String type, JsonNode p) {
        if (!type.startsWith("CultureContent") && !type.equals("ArtisanVerified")) return;

        String contentId = text(p, "contentId", null);
        if (contentId == null) return;

        String source = "culture:" + contentId;
        boolean active = "CultureContentPublished".equals(type) || "CultureContentUpdated".equals(type);
        CandidateKind kind = resolveCultureKind(text(p, "contentType", "CULTURE"));

        service.candidate(new Candidate(
                source,
                contentId,
                kind,
                text(p, "title", "Culture"),
                text(p, "categoryCode", null),
                text(p, "adminLevel1Id", null),
                text(p, "countryCode", null),
                text(p, "languageCode", null),
                number(p, "latitude"),
                number(p, "longitude"),
                0,
                active,
                active ? Instant.now() : null,
                Instant.now()
        ));
    }

    // -------------------------------------------------------------------------
    // Artwork / Artisan
    // -------------------------------------------------------------------------

    private void artisanEvent(String type, JsonNode p) {
        switch (type) {
            case "ArtworkPublished", "ArtworkUpdated"    -> upsertArtwork(p, !"ArtworkDeleted".equals(type));
            case "ArtisanVerified"                       -> upsertArtisan(p);
            case "ArtworkAvailabilityChanged"            -> {
                // Adjust popularity slightly — availability signal
                String id = text(p, "artworkId", null);
                if (id != null) {
                    boolean available = "AVAILABLE".equalsIgnoreCase(text(p, "availabilityStatus", ""));
                    service.popularity("artwork:" + id, available ? 1.0 : -1.0);
                }
            }
            default -> { /* no-op */ }
        }
    }

    private void upsertArtwork(JsonNode p, boolean active) {
        String artworkId = text(p, "artworkId", null);
        if (artworkId == null) return;

        service.candidate(new Candidate(
                "artwork:" + artworkId,
                artworkId,
                CandidateKind.ARTWORK,
                text(p, "title", "Artwork"),
                text(p, "categoryCode", null),
                text(p, "adminLevel1Id", null),
                text(p, "countryCode", null),
                text(p, "languageCode", null),
                number(p, "latitude"),
                number(p, "longitude"),
                0,
                active,
                active ? Instant.now() : null,
                Instant.now()
        ));
    }

    private void upsertArtisan(JsonNode p) {
        String artisanId = text(p, "artisanId", null);
        if (artisanId == null) return;

        service.candidate(new Candidate(
                "artisan:" + artisanId,
                artisanId,
                CandidateKind.ARTISAN,
                text(p, "displayName", "Artisan"),
                text(p, "categoryCode", null),
                text(p, "adminLevel1Id", null),
                text(p, "countryCode", null),
                text(p, "languageCode", null),
                number(p, "latitude"),
                number(p, "longitude"),
                0,
                true,
                Instant.now(),
                Instant.now()
        ));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CandidateKind resolveCultureKind(String raw) {
        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "LANGUAGE"  -> CandidateKind.LANGUAGE;
            case "TRADITION" -> CandidateKind.TRADITION;
            case "ARTWORK"   -> CandidateKind.ARTWORK;
            default          -> CandidateKind.CULTURE;
        };
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
}
