package com.yeyamo_mobile.api.gamification_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.gamification_service.application.GamificationService;
import com.yeyamo_mobile.api.gamification_service.domain.XpActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Kafka consumer for culture and artisan events.
 *
 * <p>Topics:
 * <ul>
 *   <li>{@code culture.events} — language learning, quizzes, contributions</li>
 *   <li>{@code artisan.events} — artwork story completion</li>
 * </ul>
 * </p>
 *
 * <p>Idempotency: Uses the same {@link ProcessedEventRepository} mechanism as
 * {@link GamificationEventConsumer}.  Duplicate {@code eventId} values are
 * silently skipped.</p>
 *
 * <p>Anti-fraud integration: Each mapped {@link XpActivity} is checked against
 * the daily cap before being applied.</p>
 */
@Component
public class CultureGamificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CultureGamificationEventConsumer.class);

    private final ObjectMapper            mapper;
    private final CultureEventXpPolicy   policy;
    private final CultureXpCapEnforcer   caps;
    private final GamificationService    service;
    private final ProcessedEventRepository processed;

    public CultureGamificationEventConsumer(
            ObjectMapper mapper,
            CultureEventXpPolicy policy,
            CultureXpCapEnforcer caps,
            GamificationService service,
            ProcessedEventRepository processed) {
        this.mapper    = mapper;
        this.policy    = policy;
        this.caps      = caps;
        this.service   = service;
        this.processed = processed;
    }

    @KafkaListener(
        topics  = "${yeyamo.kafka.topics.culture-events:culture.events}",
        groupId = "${spring.kafka.consumer.group-id:gamification-service}")
    @Transactional
    public void culture(String raw) throws Exception {
        process(raw);
    }

    @KafkaListener(
        topics  = "${yeyamo.kafka.topics.artisan-events:artisan.events}",
        groupId = "${spring.kafka.consumer.group-id:gamification-service}")
    @Transactional
    public void artisan(String raw) throws Exception {
        process(raw);
    }

    // -------------------------------------------------------------------------

    public void process(String raw) throws Exception {
        JsonNode event = mapper.readTree(raw);

        UUID id = uuid(event, "eventId");
        if (id == null) {
            log.warn("Received culture event with no eventId — skipping");
            return;
        }
        if (processed.existsById(id)) return;
        if (event.path("eventVersion").asInt(0) != 1)
            throw new IllegalArgumentException("Unsupported event version");

        String type        = text(event, "eventType");
        String correlation = text(event, "correlationId");

        Optional<XpActivity> activityOpt = policy.map(event);
        if (activityOpt.isPresent()) {
            XpActivity activity = activityOpt.get();

            // Daily cap enforcement (anti-fraud)
            if (!caps.allowAndRecord(activity.userId(), activity.reason(), activity.points())) {
                log.info("Daily XP cap reached: user={} action={} — XP not awarded",
                         activity.userId(), activity.reason());
                // Still mark as processed so we don't retry this event
            } else {
                service.apply(activity, correlation);
            }
        }

        // Dedup: mark processed regardless of whether XP was awarded
        processed.save(new ProcessedEventEntity(id, type != null ? type : "unknown"));
    }

    private UUID uuid(JsonNode n, String f) {
        try { return UUID.fromString(text(n, f)); } catch (Exception e) { return null; }
    }
    private String text(JsonNode n, String f) {
        JsonNode v = n.get(f); return (v == null || v.isNull()) ? null : v.asText();
    }
}
