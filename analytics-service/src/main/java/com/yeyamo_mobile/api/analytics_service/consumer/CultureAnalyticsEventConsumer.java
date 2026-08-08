package com.yeyamo_mobile.api.analytics_service.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.analytics_service.application.service.CultureAnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Consumes culture and artisan Kafka events and updates the daily projection tables.
 *
 * <p><b>Idempotency</b>: Each {@code eventId} is tracked in an in-memory set
 * (backed by a small LRU-like bounded map) so duplicate events within the same
 * JVM lifetime are ignored.  Cross-restart deduplication is handled by the
 * {@code analytics_processing_state} table (not yet wired here; safe to extend).</p>
 *
 * <p><b>Privacy</b>: Only anonymous aggregates are stored.
 * User identifiers received in event payloads are never persisted.</p>
 */
@Component
public class CultureAnalyticsEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CultureAnalyticsEventConsumer.class);
    private static final int DEDUP_WINDOW = 10_000;

    private final ObjectMapper             mapper;
    private final CultureAnalyticsService  service;
    /** Bounded dedup window: eventId → true */
    private final ConcurrentMap<String, Boolean> seen = new ConcurrentHashMap<>(DEDUP_WINDOW);

    public CultureAnalyticsEventConsumer(ObjectMapper mapper,
                                          CultureAnalyticsService service) {
        this.mapper  = mapper;
        this.service = service;
    }

    @KafkaListener(
        topics  = "${yeyamo.kafka.topics.culture-events:culture.events}",
        groupId = "${spring.kafka.consumer.group-id:analytics-service}")
    public void culture(String raw) {
        consume(raw);
    }

    @KafkaListener(
        topics  = "${yeyamo.kafka.topics.artisan-events:artisan.events}",
        groupId = "${spring.kafka.consumer.group-id:analytics-service}")
    public void artisan(String raw) {
        consume(raw);
    }

    // -------------------------------------------------------------------------

    public void consume(String raw) {
        try {
            JsonNode event = mapper.readTree(raw);
            String eventId = text(event, "eventId");
            if (eventId == null) return;

            // Idempotent dedup
            if (seen.putIfAbsent(eventId, Boolean.TRUE) != null) {
                log.debug("Duplicate analytics event {} skipped", eventId);
                return;
            }
            // Trim window to avoid unbounded growth
            if (seen.size() > DEDUP_WINDOW) seen.clear();

            String type = text(event, "eventType");
            if (type == null) return;

            JsonNode p    = event.path("payload");
            LocalDate day = LocalDate.now(ZoneOffset.UTC);

            switch (type) {
                case "LanguageLessonCompleted" -> {
                    String lang   = text(p, "languageCode");
                    String country= text(p, "countryCode");
                    if (lang != null)
                        service.upsertLanguageLearning(day, lang, country, 1L, 0L);
                }
                case "DailyWordCompleted" -> {
                    String lang   = text(p, "languageCode");
                    String country= text(p, "countryCode");
                    if (lang != null)
                        service.upsertLanguageLearning(day, lang, country, 0L, 1L);
                }
                case "ArtworkStoryCompleted" -> {
                    String artworkId = text(p, "artworkId");
                    String artisanId = text(p, "artisanId");
                    if (artworkId != null && artisanId != null)
                        service.upsertArtworkView(day, artworkId, artisanId);
                }
                case "TranslationVerified" -> {
                    String country = text(p, "countryCode");
                    service.upsertContribution(day, "TRANSLATION", country, true);
                }
                case "TranslationProposed" -> {
                    String country = text(p, "countryCode");
                    service.upsertContribution(day, "TRANSLATION", country, false);
                }
                case "OralHistoryContributed" -> {
                    String country = text(p, "countryCode");
                    service.upsertContribution(day, "ORAL_HISTORY", country, false);
                }
                case "CultureChallengeSubmitted" -> {
                    String country = text(p, "countryCode");
                    service.upsertContribution(day, "CHALLENGE_SUBMISSION", country, false);
                }
                case "CultureContentVerified" -> {
                    String country = text(p, "countryCode");
                    service.upsertContribution(day, "CULTURE_CONTENT", country, true);
                }
                default -> log.trace("No analytics projection for event type: {}", type);
            }

        } catch (Exception e) {
            log.error("Culture analytics event processing failed", e);
        }
    }

    private String text(JsonNode n, String f) {
        JsonNode v = n.get(f);
        return (v == null || v.isNull()) ? null : v.asText();
    }
}
