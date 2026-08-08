package com.yeyamo_mobile.api.gamification_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yeyamo_mobile.api.gamification_service.application.GamificationService;
import com.yeyamo_mobile.api.gamification_service.application.badge.CultureBadgeRule;
import com.yeyamo_mobile.api.gamification_service.domain.BadgeDefinition;
import com.yeyamo_mobile.api.gamification_service.domain.Progress;
import com.yeyamo_mobile.api.gamification_service.infrastructure.messaging.CultureEventXpPolicy;
import com.yeyamo_mobile.api.gamification_service.infrastructure.messaging.CultureGamificationEventConsumer;
import com.yeyamo_mobile.api.gamification_service.infrastructure.messaging.CultureXpCapEnforcer;
import com.yeyamo_mobile.api.gamification_service.infrastructure.messaging.ProcessedEventEntity;
import com.yeyamo_mobile.api.gamification_service.infrastructure.messaging.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Culture & Artisan gamification extensions.
 * Pure JUnit 5 — no Spring context.
 */
class CultureGamificationTests {

    private final ObjectMapper mapper = new ObjectMapper();

    // =========================================================================
    // CultureEventXpPolicy — mapping and anti-fraud
    // =========================================================================

    private CultureEventXpPolicy policyWithPoints(int pts) {
        // Use test constructor — no DB, no @PostConstruct
        return new CultureEventXpPolicy(Map.of(
                "LANGUAGE_LESSON_COMPLETED",    pts,
                "DAILY_WORD_COMPLETED",         10,
                "CULTURE_QUIZ_COMPLETED",       15,
                "CULTURE_CHALLENGE_SUBMITTED",  25,
                "TRANSLATION_PROPOSED",         15,
                "TRANSLATION_VERIFIED",         pts,
                "ORAL_HISTORY_CONTRIBUTED",     50,
                "CULTURE_CONTENT_VERIFIED",     20,
                "ARTWORK_STORY_COMPLETED",      15));
    }

    @Test
    void languageLessonCompleted_mapsToXpActivity() throws Exception {
        var policy = policyWithPoints(20);
        var event  = event("LanguageLessonCompleted",
                Map.of("userId", "u1", "lessonId", "lesson-abc"));

        var result = policy.map(event);
        assertThat(result).isPresent();
        assertThat(result.get().reason()).isEqualTo("LANGUAGE_LESSON_COMPLETED");
        assertThat(result.get().points()).isEqualTo(20); // default
        assertThat(result.get().userId()).isEqualTo("u1");
        assertThat(result.get().sourceId()).isEqualTo("lesson-abc");
    }

    @Test
    void emptySourceId_isRejected() throws Exception {
        var policy = policyWithPoints(20);
        var event  = event("LanguageLessonCompleted",
                Map.of("userId", "u1", "lessonId", "   "));

        // blank lessonId should cause required() to throw → empty
        var result = policy.map(event);
        assertThat(result).isEmpty();
    }

    @Test
    void translationVerified_selfValidation_isRejected() throws Exception {
        var policy = policyWithPoints(30);
        var event  = event("TranslationVerified",
                Map.of("verifierId", "u1", "proposerId", "u1",
                       "translationId", "t-123"));

        var result = policy.map(event);
        assertThat(result).as("Self-validation must produce no XP").isEmpty();
    }

    @Test
    void translationVerified_differentUsers_isAllowed() throws Exception {
        var policy = policyWithPoints(30);
        var event  = event("TranslationVerified",
                Map.of("verifierId", "expert-1", "proposerId", "contributor-1",
                       "translationId", "t-456"));

        var result = policy.map(event);
        assertThat(result).isPresent();
        assertThat(result.get().userId()).isEqualTo("contributor-1");
        assertThat(result.get().reason()).isEqualTo("TRANSLATION_VERIFIED");
    }

    @Test
    void cultureContentVerified_selfValidation_isRejected() throws Exception {
        var policy = policyWithPoints(20);
        var event  = event("CultureContentVerified",
                Map.of("verifierId", "author-1", "authorId", "author-1",
                       "contentId", "cnt-1"));

        var result = policy.map(event);
        assertThat(result).isEmpty();
    }

    @Test
    void unknownEventType_returnsEmpty() throws Exception {
        var policy = policyWithPoints(10);
        var event  = event("UnknownCultureEvent", Map.of("userId", "u1", "id", "x"));

        var result = policy.map(event);
        assertThat(result).isEmpty();
    }

    // =========================================================================
    // CultureGamificationEventConsumer — dedup and cap
    // =========================================================================

    @Test
    void duplicateEventId_isSkippedWithoutApplyingXp() throws Exception {
        var policy   = mock(CultureEventXpPolicy.class);
        var caps     = mock(CultureXpCapEnforcer.class);
        var gamSvc   = mock(GamificationService.class);
        var repo     = mock(ProcessedEventRepository.class);

        UUID eid = UUID.randomUUID();
        when(repo.existsById(eid)).thenReturn(true); // already processed

        var consumer = new CultureGamificationEventConsumer(
                mapper, policy, caps, gamSvc, repo);

        consumer.process(rawEvent("LanguageLessonCompleted", eid,
                Map.of("userId", "u1", "lessonId", "l1")));

        verify(policy, never()).map(any());
        verify(gamSvc, never()).apply(any(), any());
    }

    @Test
    void capReached_xpNotAwarded_butEventMarkedProcessed() throws Exception {
        var policy   = mock(CultureEventXpPolicy.class);
        var caps     = mock(CultureXpCapEnforcer.class);
        var gamSvc   = mock(GamificationService.class);
        var repo     = mock(ProcessedEventRepository.class);

        UUID eid = UUID.randomUUID();
        when(repo.existsById(eid)).thenReturn(false);

        var activity = new com.yeyamo_mobile.api.gamification_service.domain.XpActivity(
                eid, "u1", 20, "LANGUAGE_LESSON_COMPLETED", "l-1",
                "LESSON", null, java.time.Instant.now());
        when(policy.map(any())).thenReturn(Optional.of(activity));
        when(caps.allowAndRecord("u1", "LANGUAGE_LESSON_COMPLETED", 20)).thenReturn(false);

        var consumer = new CultureGamificationEventConsumer(
                mapper, policy, caps, gamSvc, repo);

        consumer.process(rawEvent("LanguageLessonCompleted", eid,
                Map.of("userId", "u1", "lessonId", "l-1")));

        verify(gamSvc, never()).apply(any(), any());
        // Verify save was called with a non-null ProcessedEventEntity
        verify(repo).save(argThat(e -> e instanceof ProcessedEventEntity));
    }

    @Test
    void withinCap_xpIsAwarded() throws Exception {
        var policy = mock(CultureEventXpPolicy.class);
        var caps   = mock(CultureXpCapEnforcer.class);
        var gamSvc = mock(GamificationService.class);
        var repo   = mock(ProcessedEventRepository.class);

        UUID eid = UUID.randomUUID();
        when(repo.existsById(eid)).thenReturn(false);

        var activity = new com.yeyamo_mobile.api.gamification_service.domain.XpActivity(
                eid, "u1", 15, "CULTURE_QUIZ_COMPLETED", "q-1",
                "QUIZ", null, java.time.Instant.now());
        when(policy.map(any())).thenReturn(Optional.of(activity));
        when(caps.allowAndRecord("u1", "CULTURE_QUIZ_COMPLETED", 15)).thenReturn(true);

        var consumer = new CultureGamificationEventConsumer(
                mapper, policy, caps, gamSvc, repo);

        consumer.process(rawEvent("CultureQuizCompleted", eid,
                Map.of("userId", "u1", "quizId", "q-1")));

        verify(gamSvc).apply(eq(activity), any());
    }

    // =========================================================================
    // CultureBadgeRule — badge thresholds
    // =========================================================================

    private final CultureBadgeRule badgeRule = new CultureBadgeRule();

    @Test
    void languageExplorer_awarded_at_10_lessons() {
        var result = badgeRule.evaluate(progress(0), Map.of("LESSON", 10L));
        assertContains(result, "LANGUAGE_EXPLORER");
    }

    @Test
    void languageExplorer_notAwarded_below_threshold() {
        var result = badgeRule.evaluate(progress(0), Map.of("LESSON", 9L));
        assertNotContains(result, "LANGUAGE_EXPLORER");
    }

    @Test
    void storyKeeper_awarded_at_3_oral_histories() {
        var result = badgeRule.evaluate(progress(0), Map.of("ORAL_HISTORY", 3L));
        assertContains(result, "STORY_KEEPER");
    }

    @Test
    void heritageContributor_combines_oral_and_verified() {
        // 3 oral + 2 content_vfd = 5 total
        var result = badgeRule.evaluate(progress(0),
                Map.of("ORAL_HISTORY", 3L, "CONTENT_VFD", 2L));
        assertContains(result, "HERITAGE_CONTRIBUTOR");
    }

    @Test
    void heritageContributor_notAwarded_below_5() {
        var result = badgeRule.evaluate(progress(0),
                Map.of("ORAL_HISTORY", 2L, "CONTENT_VFD", 2L));
        assertNotContains(result, "HERITAGE_CONTRIBUTOR");
    }

    @Test
    void culturalTranslator_awarded_at_10_verified() {
        var result = badgeRule.evaluate(progress(0), Map.of("TRANSLATION_VFD", 10L));
        assertContains(result, "CULTURAL_TRANSLATOR");
    }

    @Test
    void artisanSupporter_awarded_at_5_artwork_stories() {
        var result = badgeRule.evaluate(progress(0), Map.of("ARTWORK_STORY", 5L));
        assertContains(result, "ARTISAN_SUPPORTER");
    }

    @Test
    void cultureAmbassador_awarded_at_500_xp() {
        var result = badgeRule.evaluate(progress(500), Map.of());
        assertContains(result, "CULTURE_AMBASSADOR");
    }

    @Test
    void cultureAmbassador_notAwarded_below_500() {
        var result = badgeRule.evaluate(progress(499), Map.of());
        assertNotContains(result, "CULTURE_AMBASSADOR");
    }

    @Test
    void noCounters_noEarnings() {
        var result = badgeRule.evaluate(progress(0), Map.of());
        assertThat(result).isEmpty();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private com.fasterxml.jackson.databind.JsonNode event(String type,
                                                           Map<String, Object> payload)
            throws Exception {
        ObjectNode n = mapper.createObjectNode();
        n.put("eventId",      UUID.randomUUID().toString());
        n.put("eventType",    type);
        n.put("eventVersion", 1);
        n.put("occurredAt",   java.time.Instant.now().toString());
        n.set("payload",      mapper.valueToTree(payload));
        return n;
    }

    private String rawEvent(String type, UUID eventId,
                             Map<String, Object> payload) throws Exception {
        ObjectNode n = mapper.createObjectNode();
        n.put("eventId",      eventId.toString());
        n.put("eventType",    type);
        n.put("eventVersion", 1);
        n.put("occurredAt",   java.time.Instant.now().toString());
        n.set("payload",      mapper.valueToTree(payload));
        return mapper.writeValueAsString(n);
    }

    private Progress progress(long xp) {
        return new Progress("user-test", xp, Progress.levelFor(xp),
                0, 0, LocalDate.now(ZoneOffset.UTC), java.time.Instant.now());
    }

    private void assertContains(List<BadgeDefinition> list, String code) {
        assertThat(list).as("Expected badge " + code)
                .anyMatch(b -> b.code().equals(code));
    }

    private void assertNotContains(List<BadgeDefinition> list, String code) {
        assertThat(list).as("Did not expect badge " + code)
                .noneMatch(b -> b.code().equals(code));
    }
}
