package com.yeyamo_mobile.api.recommendation_service.application.adventure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.recommendation_service.application.RecommendationQueryService;
import com.yeyamo_mobile.api.recommendation_service.application.port.RecommendationProjectionPort;
import com.yeyamo_mobile.api.recommendation_service.domain.AdventureRankedCandidate;
import com.yeyamo_mobile.api.recommendation_service.domain.Candidate;
import com.yeyamo_mobile.api.recommendation_service.domain.CandidateKind;
import com.yeyamo_mobile.api.recommendation_service.domain.RecommendationScore;
import com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence.AdventurePlanDayEntity;
import com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence.AdventurePlanEntity;
import com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence.AdventurePlanItemEntity;
import com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence.AdventurePlanRepository;
import com.yeyamo_mobile.shared.country.CountryConfigClient;

class AdventurePlanServiceTest {
    private final AdventurePlanRepository plans = mock(AdventurePlanRepository.class);
    private final RecommendationProjectionPort projections = mock(RecommendationProjectionPort.class);
    private final RecommendationQueryService ranking = mock(RecommendationQueryService.class);
    private final CountryConfigClient countries = mock(CountryConfigClient.class);
    private AdventurePlanService service;

    @BeforeEach
    void setUp() {
        service = new AdventurePlanService(plans, projections, ranking, countries);
        when(countries.getCountry("CM")).thenReturn(new CountryConfigClient.CountryConfig("CM", "Cameroon", "LIVE",
                true, true, true, true, true, true, true, true, true, true, "fr", "Africa/Douala", "XAF"));
        when(ranking.rankForAdventure(eq("user-1"), anyList(), anySet())).thenAnswer(invocation -> {
            List<Candidate> candidates = invocation.getArgument(1);
            return candidates.stream().map(candidate -> new AdventureRankedCandidate(candidate, new RecommendationScore(10, Map.of())))
                    .toList();
        });
    }

    @Test
    void sameDayCreatesExactlyOneDay() {
        when(projections.activeCandidates(AdventurePlanService.MAX_CANDIDATES)).thenReturn(List.of(place("one", null)));

        AdventurePlanPreviewResponse preview = service.preview("user-1", request(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 5), null));

        assertEquals(1, preview.days().size());
        assertEquals(LocalDate.of(2026, 10, 5), preview.days().getFirst().date());
        assertEquals(1, preview.days().getFirst().items().size());
    }

    @Test
    void multiDayUsesInclusiveDatesAndExcludesEventsOutsideRangeOrInactive() {
        Candidate inRange = event("in", "2026-10-06T10:00:00Z", true);
        Candidate outsideRange = event("out", "2026-10-09T10:00:00Z", true);
        Candidate cancelled = event("cancelled", "2026-10-06T11:00:00Z", false);
        when(projections.activeCandidates(AdventurePlanService.MAX_CANDIDATES)).thenReturn(List.of(inRange, outsideRange, cancelled));

        AdventurePlanPreviewResponse preview = service.preview("user-1", request(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 7), null));

        assertEquals(List.of(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 7)),
                preview.days().stream().map(AdventureDayResponse::date).toList());
        assertEquals(List.of("in"), preview.days().get(1).items().stream().map(AdventureRecommendationResponse::targetId).toList());
    }

    @Test
    void strictBudgetExcludesUnknownPriceInsteadOfTreatingItAsFree() {
        when(projections.activeCandidates(AdventurePlanService.MAX_CANDIDATES)).thenReturn(List.of(place("unknown", null)));
        AdventureBudgetRequest budget = new AdventureBudgetRequest(AdventureBudgetTier.STANDARD,
                new BigDecimal("5000"), new BigDecimal("20000"), "XAF");

        AdventurePlanPreviewResponse preview = service.preview("user-1", request(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 5), budget));

        assertTrue(preview.days().getFirst().items().isEmpty());
        assertEquals("NO_COMPATIBLE_CANDIDATES", preview.noResultsReason());
        assertTrue(preview.warnings().contains("UNKNOWN_PRICE_EXCLUDED_BY_STRICT_BUDGET"));
    }

    @Test
    void invalidDateAndTimeRangesAreRejected() {
        AdventurePlanException dates = assertThrows(AdventurePlanException.class,
                () -> service.preview("user-1", request(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 5), null)));
        assertEquals("INVALID_DATE_RANGE", dates.getCode());
        AdventurePlanRequest times = new AdventurePlanRequest("CM", LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 5),
                LocalTime.of(17, 0), LocalTime.of(9, 0), AdventurePartyType.SOLO, List.of(), null);
        assertEquals("INVALID_TIME_RANGE", assertThrows(AdventurePlanException.class,
                () -> service.preview("user-1", times)).getCode());
    }

    @Test
    void plansAreNeverReadWithoutMatchingOwner() {
        UUID planId = UUID.randomUUID();
        when(plans.findDetailByIdAndUserId(planId, "user-1")).thenReturn(Optional.empty());
        assertEquals("ADVENTURE_PLAN_NOT_FOUND", assertThrows(AdventurePlanException.class,
                () -> service.get("user-1", planId)).getCode());
    }

    @Test
    void skipPersistsExclusionAndUsesOnlyAnotherCandidateAsReplacement() {
        UUID planId = UUID.randomUUID();
        UUID originalRecommendation = UUID.randomUUID();
        AdventurePlanEntity plan = savedPlan(planId, originalRecommendation);
        when(plans.findDetailByIdAndUserId(planId, "user-1")).thenReturn(Optional.of(plan));
        when(projections.activeCandidates(AdventurePlanService.MAX_CANDIDATES)).thenReturn(List.of(place("original", null), place("replacement", null)));

        AdventureSkipResponse result = service.skip("user-1", planId, originalRecommendation);

        assertEquals(originalRecommendation, result.skippedRecommendationId());
        assertEquals("replacement", result.replacement().targetId());
        assertNull(result.reasonCode());
        assertTrue(plan.getDays().getFirst().getItems().getFirst().getSkippedAt() != null);
        assertEquals(1, service.get("user-1", planId).days().getFirst().items().size());
        assertEquals(result.replacement().recommendationId(), service.get("user-1", planId).days().getFirst().items().getFirst().recommendationId());
    }

    private AdventurePlanRequest request(LocalDate start, LocalDate end, AdventureBudgetRequest budget) {
        return new AdventurePlanRequest("CM", start, end, LocalTime.of(9, 0), LocalTime.of(17, 0),
                AdventurePartyType.SOLO, List.of("culture"), budget);
    }

    private Candidate place(String id, BigDecimal price) {
        return new Candidate("catalog:" + id, id, CandidateKind.PLACE, id, "culture", null, "CM", "fr",
                null, null, 0, true, Instant.now(), Instant.now(), price, price == null ? null : "XAF", null,
                "Douala", null, null);
    }

    private Candidate event(String id, String start, boolean active) {
        Instant startsAt = Instant.parse(start);
        return new Candidate("event:" + id, id, CandidateKind.EVENT, id, "events", null, "CM", "fr",
                null, null, 0, active, startsAt, Instant.now(), null, null, null, "Douala", startsAt,
                startsAt.plusSeconds(3600));
    }

    private AdventurePlanEntity savedPlan(UUID id, UUID recommendationId) {
        AdventurePlanEntity plan = new AdventurePlanEntity();
        plan.setId(id); plan.setUserId("user-1"); plan.setCountryCode("CM");
        plan.setStartDate(LocalDate.of(2026, 10, 5)); plan.setEndDate(LocalDate.of(2026, 10, 5));
        plan.setStartTime(LocalTime.of(9, 0)); plan.setEndTime(LocalTime.of(17, 0)); plan.setPartyType(AdventurePartyType.SOLO);
        AdventurePlanDayEntity day = new AdventurePlanDayEntity(); day.setId(UUID.randomUUID()); day.setDate(plan.getStartDate()); day.setPosition(0); plan.addDay(day);
        AdventurePlanItemEntity item = new AdventurePlanItemEntity(); item.setId(UUID.randomUUID()); item.setRecommendationId(recommendationId);
        item.setSourceId("catalog:original"); item.setTargetType(AdventureTargetType.PLACE); item.setTargetId("original"); item.setPosition(0);
        item.setSnapshotTitle("original"); item.setAvailabilityStatus(AdventureAvailabilityStatus.UNKNOWN); item.setReasonCodes("COUNTRY_MATCH"); day.addItem(item);
        return plan;
    }
}
