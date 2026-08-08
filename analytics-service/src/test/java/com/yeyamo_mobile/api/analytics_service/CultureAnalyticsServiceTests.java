package com.yeyamo_mobile.api.analytics_service;

import com.yeyamo_mobile.api.analytics_service.application.dto.CultureOverviewResponse;
import com.yeyamo_mobile.api.analytics_service.application.service.CultureAnalyticsService;
import com.yeyamo_mobile.api.analytics_service.domain.model.*;
import com.yeyamo_mobile.api.analytics_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CultureAnalyticsService.
 * Pure JUnit 5 — no Spring context, no DB.
 */
class CultureAnalyticsServiceTests {

    private CultureEngagementDailyRepository  engRepo;
    private LanguageLearningDailyRepository   langRepo;
    private ArtworkPopularityDailyRepository  artworkRepo;
    private ArtisanKpisDailyRepository        artisanRepo;
    private CultureContributionDailyRepository contribRepo;

    private CultureAnalyticsService service;

    @BeforeEach
    void setUp() {
        engRepo     = mock(CultureEngagementDailyRepository.class);
        langRepo    = mock(LanguageLearningDailyRepository.class);
        artworkRepo = mock(ArtworkPopularityDailyRepository.class);
        artisanRepo = mock(ArtisanKpisDailyRepository.class);
        contribRepo = mock(CultureContributionDailyRepository.class);

        service = new CultureAnalyticsService(
                engRepo, langRepo, artworkRepo, artisanRepo, contribRepo);

        // Default stubs returning empty collections so no NPE
        when(engRepo.findByCountryCodeAndAggregationDateBetween(any(), any(), any()))
                .thenReturn(List.of());
        when(engRepo.sumContentViews(any(), any(), any())).thenReturn(0L);
        when(langRepo.topLanguagesByLearners(any(), any())).thenReturn(List.of());
        when(langRepo.findByLanguageCodeAndAggregationDateBetweenOrderByAggregationDateDesc(
                any(), any(), any())).thenReturn(List.of());
        when(langRepo.findByCountryCodeAndAggregationDateBetweenOrderByAggregationDateDesc(
                any(), any(), any())).thenReturn(List.of());
        when(artworkRepo.findTopByDateRange(any(), any())).thenReturn(List.of());
        when(artworkRepo.findByArtworkIdAndAggregationDateBetweenOrderByAggregationDateDesc(
                any(), any(), any())).thenReturn(List.of());
        when(artworkRepo.findByArtisanIdAndAggregationDateBetweenOrderByAggregationDateDesc(
                any(), any(), any())).thenReturn(List.of());
        when(artisanRepo.findByArtisanIdAndAggregationDateBetweenOrderByAggregationDateDesc(
                any(), any(), any())).thenReturn(List.of());
        when(contribRepo.findByAggregationDateBetweenOrderByAggregationDateDesc(any(), any()))
                .thenReturn(List.of());
        when(contribRepo.findByCountryCodeAndAggregationDateBetween(any(), any(), any()))
                .thenReturn(List.of());
        when(contribRepo.sumByTypeAndDateRange(any(), any(), any())).thenReturn(List.of());
    }

    // =========================================================================
    // Overview
    // =========================================================================

    @Test
    void getCultureOverview_returnsValidResponse() {
        when(engRepo.sumContentViews(any(), any(), any())).thenReturn(1500L);
        when(langRepo.topLanguagesByLearners(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"bm", 250L}));

        CultureOverviewResponse overview = service.getCultureOverview();

        assertThat(overview).isNotNull();
        assertThat(overview.getLast30Days()).isNotNull();
        assertThat(overview.getLast30Days().getTotalViews()).isEqualTo(1500L);
        assertThat(overview.getTopLanguages()).hasSize(1);
        assertThat(overview.getTopLanguages().get(0).getLanguageCode()).isEqualTo("bm");
    }

    @Test
    void getCultureOverview_emptyData_returnsZeros() {
        CultureOverviewResponse overview = service.getCultureOverview();
        assertThat(overview.getLast30Days().getTotalViews()).isZero();
        assertThat(overview.getTopLanguages()).isEmpty();
        assertThat(overview.getTrendingArtworks()).isEmpty();
    }

    // =========================================================================
    // Language analytics
    // =========================================================================

    @Test
    void getLanguageAnalytics_aggregatesLessonsAndWords() {
        when(langRepo.findByLanguageCodeAndAggregationDateBetweenOrderByAggregationDateDesc(
                eq("bm"), any(), any()))
                .thenReturn(List.of(
                        row("bm", LocalDate.now(ZoneOffset.UTC).minusDays(1), 5L, 10L, 2L),
                        row("bm", LocalDate.now(ZoneOffset.UTC),               3L,  6L, 1L)));

        Map<String, Object> result = service.getLanguageAnalytics("bm", null, null);

        assertThat(result.get("languageCode")).isEqualTo("bm");
        assertThat(result.get("totalLessonsCompleted")).isEqualTo(8L);
        assertThat(result.get("totalWordsLearned")).isEqualTo(16L);
    }

    @Test
    void getLanguageAnalytics_dateRangeFiltering() {
        LocalDate from = LocalDate.now(ZoneOffset.UTC).minusDays(7);
        LocalDate to   = LocalDate.now(ZoneOffset.UTC);

        service.getLanguageAnalytics("fr", from, to);

        verify(langRepo).findByLanguageCodeAndAggregationDateBetweenOrderByAggregationDateDesc(
                eq("fr"), eq(from), eq(to));
    }

    @Test
    void getLanguageAnalytics_defaultsTo30DayRange() {
        service.getLanguageAnalytics("wo", null, null);

        verify(langRepo).findByLanguageCodeAndAggregationDateBetweenOrderByAggregationDateDesc(
                eq("wo"),
                argThat(d -> d.isBefore(LocalDate.now(ZoneOffset.UTC).minusDays(28))),
                any());
    }

    // =========================================================================
    // Country analytics
    // =========================================================================

    @Test
    void getCountryAnalytics_aggregatesEngagementAndContributions() {
        when(engRepo.findByCountryCodeAndAggregationDateBetween(eq("SN"), any(), any()))
                .thenReturn(List.of(
                        engagement("SN", 100L, 20L),
                        engagement("SN", 80L,  15L)));
        when(contribRepo.findByCountryCodeAndAggregationDateBetween(eq("SN"), any(), any()))
                .thenReturn(List.of(contribution("SN", "TRANSLATION", 5L, 3L)));

        Map<String, Object> result = service.getCountryAnalytics("SN", null, null);

        assertThat(result.get("countryCode")).isEqualTo("SN");
        assertThat(result.get("totalContentViews")).isEqualTo(180L);
        assertThat(result.get("totalContributions")).isEqualTo(5L);
    }

    @Test
    void getCountryAnalytics_differentCountriesAreIsolated() {
        // SN query should not return ML data
        when(engRepo.findByCountryCodeAndAggregationDateBetween(eq("ML"), any(), any()))
                .thenReturn(List.of(engagement("ML", 50L, 10L)));

        Map<String, Object> sn = service.getCountryAnalytics("SN", null, null);
        Map<String, Object> ml = service.getCountryAnalytics("ML", null, null);

        assertThat(sn.get("totalContentViews")).isEqualTo(0L);
        assertThat(ml.get("totalContentViews")).isEqualTo(50L);
    }

    // =========================================================================
    // Artisan analytics
    // =========================================================================

    @Test
    void getArtisanAnalytics_sumsSalesAndViews() {
        when(artisanRepo.findByArtisanIdAndAggregationDateBetweenOrderByAggregationDateDesc(
                eq("artisan-1"), any(), any()))
                .thenReturn(List.of(
                        artisan("artisan-1", 3L, 150_000L, 500L),
                        artisan("artisan-1", 2L, 100_000L, 300L)));

        Map<String, Object> result = service.getArtisanAnalytics("artisan-1", null, null);

        assertThat(result.get("artisanId")).isEqualTo("artisan-1");
        assertThat(result.get("totalSales")).isEqualTo(5L);
        assertThat(result.get("totalViews")).isEqualTo(800L);
    }

    // =========================================================================
    // Artwork analytics
    // =========================================================================

    @Test
    void getArtworkAnalytics_sumsPurchasesAndViews() {
        when(artworkRepo.findByArtworkIdAndAggregationDateBetweenOrderByAggregationDateDesc(
                eq("art-42"), any(), any()))
                .thenReturn(List.of(
                        artwork("art-42", 200L, 10L, 2L)));

        Map<String, Object> result = service.getArtworkAnalytics("art-42", null, null);

        assertThat(result.get("artworkId")).isEqualTo("art-42");
        assertThat(result.get("totalViews")).isEqualTo(200L);
        assertThat(result.get("totalPurchases")).isEqualTo(2L);
    }

    // =========================================================================
    // Contribution analytics
    // =========================================================================

    @Test
    void getContributionAnalytics_filtersByType() {
        when(contribRepo.findByAggregationDateBetweenOrderByAggregationDateDesc(any(), any()))
                .thenReturn(List.of(
                        contribution("ML", "TRANSLATION",  10L, 8L),
                        contribution("SN", "ORAL_HISTORY",  5L, 3L)));

        Map<String, Object> result =
                service.getContributionAnalytics("TRANSLATION", null, null, null);

        // Only TRANSLATION rows should remain
        assertThat(result.get("totalContributions")).isEqualTo(10L);
        assertThat(result.get("verifiedContributions")).isEqualTo(8L);
    }

    @Test
    void getContributionAnalytics_filtersByCountry() {
        when(contribRepo.findByCountryCodeAndAggregationDateBetween(eq("GN"), any(), any()))
                .thenReturn(List.of(contribution("GN", "ORAL_HISTORY", 7L, 4L)));

        Map<String, Object> result =
                service.getContributionAnalytics(null, "GN", null, null);

        assertThat(result.get("totalContributions")).isEqualTo(7L);
    }

    // =========================================================================
    // Idempotence — upsert helpers
    // =========================================================================

    @Test
    void upsertLanguageLearning_createsRowWhenAbsent() {
        when(langRepo.findByAggregationDateAndLanguageCodeAndCountryCode(
                any(), eq("bm"), eq("ML"))).thenReturn(Optional.empty());
        when(langRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.upsertLanguageLearning(LocalDate.now(ZoneOffset.UTC), "bm", "ML", 1L, 0L);

        verify(langRepo).save(argThat(r ->
                ((LanguageLearningDaily) r).getLessonsCompleted() == 1L));
    }

    @Test
    void upsertLanguageLearning_accumulatesOnExistingRow() {
        LanguageLearningDaily existing = LanguageLearningDaily.builder()
                .aggregationDate(LocalDate.now(ZoneOffset.UTC))
                .languageCode("fr").countryCode("CI")
                .lessonsCompleted(5L).wordsLearned(20L).activeLearners(3L)
                .createdAt(Instant.now()).build();

        when(langRepo.findByAggregationDateAndLanguageCodeAndCountryCode(
                any(), eq("fr"), eq("CI"))).thenReturn(Optional.of(existing));
        when(langRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.upsertLanguageLearning(LocalDate.now(ZoneOffset.UTC), "fr", "CI", 2L, 5L);

        verify(langRepo).save(argThat(r -> {
            LanguageLearningDaily saved = (LanguageLearningDaily) r;
            return saved.getLessonsCompleted() == 7L && saved.getWordsLearned() == 25L;
        }));
    }

    @Test
    void upsertContribution_tracksVerifiedAndTotal() {
        when(contribRepo.findByAggregationDateAndCountryCodeAndContributionType(
                any(), eq("BJ"), eq("TRANSLATION"))).thenReturn(Optional.empty());
        when(contribRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.upsertContribution(LocalDate.now(ZoneOffset.UTC),
                "TRANSLATION", "BJ", true);

        verify(contribRepo).save(argThat(r -> {
            CultureContributionDaily c = (CultureContributionDaily) r;
            return c.getTotalContributions() == 1L
                && c.getVerifiedContributions() == 1L
                && "TRANSLATION".equals(c.getContributionType());
        }));
    }

    // =========================================================================
    // Privacy — no PII in aggregated results
    // =========================================================================

    @Test
    void languageAnalytics_doesNotExposeUserIds() {
        when(langRepo.findByLanguageCodeAndAggregationDateBetweenOrderByAggregationDateDesc(
                any(), any(), any()))
                .thenReturn(List.of(row("bm", LocalDate.now(ZoneOffset.UTC), 3L, 6L, 1L)));

        Map<String, Object> result = service.getLanguageAnalytics("bm", null, null);

        // No per-user data in response
        assertThat(result).doesNotContainKey("userId");
        assertThat(result).doesNotContainKey("user");
        assertThat(result).doesNotContainKey("email");

        // Breakdown rows also clean
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breakdown =
                (List<Map<String, Object>>) result.get("dailyBreakdown");
        assertThat(breakdown).isNotEmpty();
        assertThat(breakdown.get(0)).doesNotContainKey("userId");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private LanguageLearningDaily row(String lang, LocalDate date,
                                       long lessons, long words, long learners) {
        return LanguageLearningDaily.builder()
                .aggregationDate(date).languageCode(lang)
                .lessonsCompleted(lessons).wordsLearned(words)
                .activeLearners(learners).createdAt(Instant.now()).build();
    }

    private CultureEngagementDaily engagement(String country, long views, long likes) {
        return CultureEngagementDaily.builder()
                .aggregationDate(LocalDate.now(ZoneOffset.UTC))
                .countryCode(country)
                .contentViews(views).likes(likes)
                .createdAt(Instant.now()).build();
    }

    private CultureContributionDaily contribution(String country, String type,
                                                    long total, long verified) {
        return CultureContributionDaily.builder()
                .aggregationDate(LocalDate.now(ZoneOffset.UTC))
                .countryCode(country).contributionType(type)
                .totalContributions(total).verifiedContributions(verified)
                .createdAt(Instant.now()).build();
    }

    private ArtisanKpisDaily artisan(String id, long sales, long revenue, long views) {
        return ArtisanKpisDaily.builder()
                .aggregationDate(LocalDate.now(ZoneOffset.UTC))
                .artisanId(id).sales(sales)
                .revenue(BigDecimal.valueOf(revenue)).totalViews(views)
                .newFollowers(10L).createdAt(Instant.now()).build();
    }

    private ArtworkPopularityDaily artwork(String id, long views, long likes, long purchases) {
        return ArtworkPopularityDaily.builder()
                .aggregationDate(LocalDate.now(ZoneOffset.UTC))
                .artworkId(id).artisanId("artisan-x")
                .views(views).likes(likes).purchases(purchases)
                .createdAt(Instant.now()).build();
    }
}
