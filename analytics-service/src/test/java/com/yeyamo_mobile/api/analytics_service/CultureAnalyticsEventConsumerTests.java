package com.yeyamo_mobile.api.analytics_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yeyamo_mobile.api.analytics_service.application.service.CultureAnalyticsService;
import com.yeyamo_mobile.api.analytics_service.consumer.CultureAnalyticsEventConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CultureAnalyticsEventConsumer.
 * Verifies idempotence, routing, and projection calls.
 */
class CultureAnalyticsEventConsumerTests {

    private CultureAnalyticsService service;
    private CultureAnalyticsEventConsumer consumer;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service  = mock(CultureAnalyticsService.class);
        consumer = new CultureAnalyticsEventConsumer(mapper, service);
    }

    // =========================================================================
    // Idempotence
    // =========================================================================

    @Test
    void duplicateEvent_ignoredAfterFirstProcessing() throws Exception {
        UUID eventId = UUID.randomUUID();
        String raw = event("LanguageLessonCompleted", eventId,
                Map.of("languageCode", "bm", "countryCode", "ML"));

        consumer.consume(raw);
        consumer.consume(raw); // duplicate

        // Service must be called exactly once
        verify(service, times(1)).upsertLanguageLearning(any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    void differentEventIds_bothProcessed() throws Exception {
        String raw1 = event("LanguageLessonCompleted", UUID.randomUUID(),
                Map.of("languageCode", "fr", "countryCode", "SN"));
        String raw2 = event("LanguageLessonCompleted", UUID.randomUUID(),
                Map.of("languageCode", "fr", "countryCode", "SN"));

        consumer.consume(raw1);
        consumer.consume(raw2);

        verify(service, times(2)).upsertLanguageLearning(any(), eq("fr"), eq("SN"), eq(1L), eq(0L));
    }

    // =========================================================================
    // Event routing
    // =========================================================================

    @Test
    void languageLessonCompleted_callsUpsertLanguageLearning() throws Exception {
        String raw = event("LanguageLessonCompleted", UUID.randomUUID(),
                Map.of("languageCode", "wo", "countryCode", "SN"));

        consumer.consume(raw);

        verify(service).upsertLanguageLearning(
                eq(LocalDate.now(ZoneOffset.UTC)),
                eq("wo"), eq("SN"), eq(1L), eq(0L));
    }

    @Test
    void dailyWordCompleted_callsUpsertLanguageLearningWithWordIncrement() throws Exception {
        String raw = event("DailyWordCompleted", UUID.randomUUID(),
                Map.of("languageCode", "bm", "countryCode", "ML", "wordId", "word-1"));

        consumer.consume(raw);

        verify(service).upsertLanguageLearning(
                eq(LocalDate.now(ZoneOffset.UTC)),
                eq("bm"), eq("ML"), eq(0L), eq(1L));
    }

    @Test
    void artworkStoryCompleted_callsUpsertArtworkView() throws Exception {
        String raw = event("ArtworkStoryCompleted", UUID.randomUUID(),
                Map.of("artworkId", "art-1", "artisanId", "artisan-1"));

        consumer.consume(raw);

        verify(service).upsertArtworkView(
                eq(LocalDate.now(ZoneOffset.UTC)),
                eq("art-1"), eq("artisan-1"));
    }

    @Test
    void translationVerified_callsUpsertContributionVerified() throws Exception {
        String raw = event("TranslationVerified", UUID.randomUUID(),
                Map.of("translationId", "t-1", "countryCode", "CI"));

        consumer.consume(raw);

        verify(service).upsertContribution(
                eq(LocalDate.now(ZoneOffset.UTC)),
                eq("TRANSLATION"), eq("CI"), eq(true));
    }

    @Test
    void translationProposed_callsUpsertContributionUnverified() throws Exception {
        String raw = event("TranslationProposed", UUID.randomUUID(),
                Map.of("translationId", "t-2", "countryCode", "GN"));

        consumer.consume(raw);

        verify(service).upsertContribution(
                eq(LocalDate.now(ZoneOffset.UTC)),
                eq("TRANSLATION"), eq("GN"), eq(false));
    }

    @Test
    void oralHistoryContributed_callsUpsertContribution() throws Exception {
        String raw = event("OralHistoryContributed", UUID.randomUUID(),
                Map.of("audioId", "audio-1", "countryCode", "BJ"));

        consumer.consume(raw);

        verify(service).upsertContribution(
                eq(LocalDate.now(ZoneOffset.UTC)),
                eq("ORAL_HISTORY"), eq("BJ"), eq(false));
    }

    @Test
    void challengeSubmitted_callsUpsertContribution() throws Exception {
        String raw = event("CultureChallengeSubmitted", UUID.randomUUID(),
                Map.of("submissionId", "sub-1", "countryCode", "CM"));

        consumer.consume(raw);

        verify(service).upsertContribution(
                eq(LocalDate.now(ZoneOffset.UTC)),
                eq("CHALLENGE_SUBMISSION"), eq("CM"), eq(false));
    }

    @Test
    void cultureContentVerified_callsUpsertContributionVerified() throws Exception {
        String raw = event("CultureContentVerified", UUID.randomUUID(),
                Map.of("contentId", "c-1", "countryCode", "SN"));

        consumer.consume(raw);

        verify(service).upsertContribution(
                eq(LocalDate.now(ZoneOffset.UTC)),
                eq("CULTURE_CONTENT"), eq("SN"), eq(true));
    }

    @Test
    void unknownEventType_noServiceCall() throws Exception {
        String raw = event("SomethingElseHappened", UUID.randomUUID(),
                Map.of("id", "x1"));

        consumer.consume(raw);

        verifyNoInteractions(service);
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    @Test
    void malformedJson_doesNotPropagateException() {
        // Must not throw — just log error
        consumer.consume("{ not valid json ]]");
        verifyNoInteractions(service);
    }

    @Test
    void missingEventId_silentlyIgnored() throws Exception {
        String raw = mapper.writeValueAsString(Map.of(
                "eventType", "LanguageLessonCompleted",
                "eventVersion", 1,
                "payload", Map.of("languageCode", "fr")));

        consumer.consume(raw);
        verifyNoInteractions(service);
    }

    @Test
    void missingLanguageCode_doesNotCallService() throws Exception {
        // languageCode is null — service should not be called
        String raw = event("LanguageLessonCompleted", UUID.randomUUID(),
                Map.of("lessonId", "l-1")); // no languageCode

        consumer.consume(raw);

        verify(service, never()).upsertLanguageLearning(any(), any(), any(), anyLong(), anyLong());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String event(String type, UUID eventId, Map<String, Object> payload) throws Exception {
        ObjectNode n = mapper.createObjectNode();
        n.put("eventId",      eventId.toString());
        n.put("eventType",    type);
        n.put("eventVersion", 1);
        n.put("occurredAt",   java.time.Instant.now().toString());
        n.set("payload",      mapper.valueToTree(payload));
        return mapper.writeValueAsString(n);
    }
}
