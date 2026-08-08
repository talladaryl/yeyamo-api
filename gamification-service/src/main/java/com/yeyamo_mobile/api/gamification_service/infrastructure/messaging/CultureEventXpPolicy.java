package com.yeyamo_mobile.api.gamification_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.yeyamo_mobile.api.gamification_service.domain.XpActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps culture and artisan Kafka events to {@link XpActivity}.
 *
 * <p>Points are loaded from the {@code xp_action_rules} table at startup so
 * they can be changed without a code deployment.  The cache is refreshed on
 * startup only; a restart is needed to pick up point changes in production
 * (acceptable trade-off vs always hitting the DB on every event).</p>
 *
 * <p>Anti-fraud rules enforced here (before reaching the ledger):
 * <ul>
 *   <li>Empty sourceId → rejected (prevents phantom submissions).</li>
 *   <li>Auto-validation guard: {@code actorId} must differ from
 *       {@code contentOwnerId} for TRANSLATION_VERIFIED and
 *       CULTURE_CONTENT_VERIFIED.</li>
 *   <li>Daily caps are enforced in {@link CultureXpCapEnforcer}.</li>
 *   <li>Duplicate eventId protection is handled by the idempotent
 *       {@code gamification_processed_events} table (existing mechanism).</li>
 * </ul>
 * </p>
 */
@Component
public class CultureEventXpPolicy {

    private static final Logger log = LoggerFactory.getLogger(CultureEventXpPolicy.class);

    /** action_code → points loaded from DB */
    private final Map<String, Integer> pointsCache = new ConcurrentHashMap<>();

    private final JdbcTemplate jdbc;

    @org.springframework.beans.factory.annotation.Autowired
    public CultureEventXpPolicy(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void loadRules() {
        try {
            jdbc.query(
                "SELECT action_code, points FROM xp_action_rules WHERE enabled = true",
                rs -> {
                    pointsCache.put(rs.getString("action_code"), rs.getInt("points"));
                }
            );
            log.info("Loaded {} XP action rules from DB", pointsCache.size());
        } catch (Exception e) {
            log.warn("Could not load XP action rules from DB (using defaults): {}", e.getMessage());
            loadDefaults();
        }
    }

    /** Reload rules at runtime (e.g. via admin endpoint). */
    public void reload() {
        pointsCache.clear();
        loadRules();
    }

    /** Constructor for tests: pre-loads a fixed points map without touching the DB. */
    public CultureEventXpPolicy(Map<String, Integer> fixedPoints) {
        this.jdbc = null;
        this.pointsCache.putAll(fixedPoints);
    }

    // -------------------------------------------------------------------------
    // Public mapping API
    // -------------------------------------------------------------------------

    public Optional<XpActivity> map(JsonNode event) {
        String type    = text(event, "eventType");
        JsonNode p     = event.path("payload");
        UUID   eventId = uuid(event, "eventId");
        Instant at     = instant(event, "occurredAt");

        if (eventId == null || type == null) return Optional.empty();

        return switch (type) {

            case "LanguageLessonCompleted" -> {
                String userId   = required(p, "userId");
                String sourceId = text(p, "lessonId");
                if (empty(sourceId)) yield Optional.empty();
                yield activity(eventId, userId, "LANGUAGE_LESSON_COMPLETED",
                        sourceId, "LESSON", null, at);
            }
            case "DailyWordCompleted" -> {
                String userId   = required(p, "userId");
                String sourceId = text(p, "wordId");
                if (empty(sourceId)) yield Optional.empty();
                yield activity(eventId, userId, "DAILY_WORD_COMPLETED",
                        sourceId, "DAILY_WORD", null, at);
            }
            case "CultureQuizCompleted" -> {
                String userId   = required(p, "userId");
                String sourceId = text(p, "quizId");
                if (empty(sourceId)) yield Optional.empty();
                yield activity(eventId, userId, "CULTURE_QUIZ_COMPLETED",
                        sourceId, "QUIZ", null, at);
            }
            case "CultureChallengeSubmitted" -> {
                String userId   = required(p, "userId");
                String sourceId = text(p, "submissionId");
                if (empty(sourceId)) yield Optional.empty();
                // Empty submission guard: submissions with no content are rejected at source
                // We accept anything that arrives with a valid submissionId
                yield activity(eventId, userId, "CULTURE_CHALLENGE_SUBMITTED",
                        sourceId, "CHALLENGE", null, at);
            }
            case "TranslationProposed" -> {
                String userId   = required(p, "proposerId");
                String sourceId = text(p, "translationId");
                if (empty(sourceId)) yield Optional.empty();
                yield activity(eventId, userId, "TRANSLATION_PROPOSED",
                        sourceId, "TRANSLATION", null, at);
            }
            case "TranslationVerified" -> {
                // Anti-fraud: proposer ≠ verifier (no self-validation)
                String verifierId = required(p, "verifierId");
                String proposerId = text(p, "proposerId");
                String sourceId   = text(p, "translationId");
                if (empty(sourceId)) yield Optional.empty();
                if (verifierId.equals(proposerId)) {
                    log.warn("Anti-fraud: self-validation rejected for translation {}", sourceId);
                    yield Optional.empty();
                }
                yield activity(eventId, proposerId, "TRANSLATION_VERIFIED",
                        sourceId, "TRANSLATION_VFD", null, at);
            }
            case "OralHistoryContributed" -> {
                String userId   = required(p, "contributorId");
                String sourceId = text(p, "audioId");
                if (empty(sourceId)) yield Optional.empty();
                yield activity(eventId, userId, "ORAL_HISTORY_CONTRIBUTED",
                        sourceId, "ORAL_HISTORY", null, at);
            }
            case "CultureContentVerified" -> {
                // Anti-fraud: content owner ≠ verifier
                String verifierId  = required(p, "verifierId");
                String authorId    = text(p, "authorId");
                String sourceId    = text(p, "contentId");
                if (empty(sourceId)) yield Optional.empty();
                if (verifierId.equals(authorId)) {
                    log.warn("Anti-fraud: self-validation rejected for content {}", sourceId);
                    yield Optional.empty();
                }
                yield activity(eventId, authorId, "CULTURE_CONTENT_VERIFIED",
                        sourceId, "CONTENT_VFD", null, at);
            }
            case "ArtworkStoryCompleted" -> {
                String userId   = required(p, "userId");
                String sourceId = text(p, "artworkId");
                if (empty(sourceId)) yield Optional.empty();
                yield activity(eventId, userId, "ARTWORK_STORY_COMPLETED",
                        sourceId, "ARTWORK_STORY", null, at);
            }
            default -> Optional.empty();
        };
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Optional<XpActivity> activity(UUID eventId, String userId,
                                           String actionCode, String sourceId,
                                           String counterType, String destinationId,
                                           Instant at) {
        Integer pts = pointsCache.get(actionCode);
        if (pts == null) {
            log.debug("No XP rule configured for action {}", actionCode);
            return Optional.empty();
        }
        if (empty(userId) || empty(sourceId)) return Optional.empty();
        return Optional.of(new XpActivity(eventId, userId, pts, actionCode,
                sourceId, counterType, destinationId, at));
    }

    private void loadDefaults() {
        pointsCache.put("DAILY_WORD_COMPLETED",        10);
        pointsCache.put("LANGUAGE_LESSON_COMPLETED",   20);
        pointsCache.put("CULTURE_QUIZ_COMPLETED",      15);
        pointsCache.put("CULTURE_CHALLENGE_SUBMITTED", 25);
        pointsCache.put("TRANSLATION_PROPOSED",        15);
        pointsCache.put("TRANSLATION_VERIFIED",        30);
        pointsCache.put("ORAL_HISTORY_CONTRIBUTED",    50);
        pointsCache.put("CULTURE_CONTENT_VERIFIED",    20);
        pointsCache.put("ARTWORK_STORY_COMPLETED",     15);
    }

    private UUID uuid(JsonNode n, String f) {
        try { return UUID.fromString(Objects.requireNonNull(text(n, f))); }
        catch (Exception e) { return null; }
    }
    private String required(JsonNode n, String f) {
        String v = text(n, f);
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " is required");
        return v;
    }
    private String text(JsonNode n, String f) {
        JsonNode v = n.get(f); return (v == null || v.isNull()) ? null : v.asText();
    }
    private Instant instant(JsonNode n, String f) {
        try { String v = text(n, f); return v == null ? Instant.now() : Instant.parse(v); }
        catch (Exception e) { return Instant.now(); }
    }
    private boolean empty(String v) { return v == null || v.isBlank(); }
}
