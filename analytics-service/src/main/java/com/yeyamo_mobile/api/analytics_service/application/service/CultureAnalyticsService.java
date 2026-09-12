package com.yeyamo_mobile.api.analytics_service.application.service;

import com.yeyamo_mobile.api.analytics_service.application.dto.CultureOverviewResponse;
import com.yeyamo_mobile.api.analytics_service.domain.model.*;
import com.yeyamo_mobile.api.analytics_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for culture & artisan analytics.
 *
 * <p><b>Privacy</b>: all methods return aggregated data only.
 * No individual user identifier is ever included in responses.</p>
 *
 * <p>Date ranges default to last 30 days when not specified.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CultureAnalyticsService {

    private final CultureEngagementDailyRepository  engagementRepo;
    private final LanguageLearningDailyRepository    languageRepo;
    private final ArtworkPopularityDailyRepository   artworkRepo;
    private final ArtisanKpisDailyRepository         artisanRepo;
    private final CultureContributionDailyRepository contributionRepo;

    // =========================================================================
    // Overview
    // =========================================================================

    public CultureOverviewResponse getCultureOverview() {
        LocalDate today = today();
        LocalDate d7    = today.minusDays(7);
        LocalDate d30   = today.minusDays(30);

        return CultureOverviewResponse.builder()
                .today(periodMetrics(today, today))
                .last7Days(periodMetrics(d7, today))
                .last30Days(periodMetrics(d30, today))
                .topLanguages(topLanguages(d7, today, 10))
                .trendingArtworks(trendingArtworks(d7, today, 10))
                .build();
    }

    // =========================================================================
    // Language analytics
    // =========================================================================

    public Map<String, Object> getLanguageAnalytics(
            String languageCode, LocalDate start, LocalDate end) {

        LocalDate from = start != null ? start : today().minusDays(30);
        LocalDate to   = end   != null ? end   : today();

        List<LanguageLearningDaily> rows =
                languageRepo.findByLanguageCodeAndAggregationDateBetweenOrderByAggregationDateDesc(
                        languageCode, from, to);

        long totalLessons = rows.stream().mapToLong(r ->
                r.getLessonsCompleted() != null ? r.getLessonsCompleted() : 0L).sum();
        long totalWords   = rows.stream().mapToLong(r ->
                r.getWordsLearned()   != null ? r.getWordsLearned()   : 0L).sum();
        long activeLearners = rows.stream().mapToLong(r ->
                r.getActiveLearners() != null ? r.getActiveLearners() : 0L).sum();
        long quizzesTaken = rows.stream().mapToLong(r ->
                r.getQuizzesTaken()   != null ? r.getQuizzesTaken()   : 0L).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("languageCode",    languageCode);
        result.put("period",          Map.of("from", from, "to", to));
        result.put("totalLessonsCompleted", totalLessons);
        result.put("totalWordsLearned",     totalWords);
        result.put("totalActiveLearners",   activeLearners);
        result.put("totalQuizzesTaken",     quizzesTaken);
        result.put("dailyBreakdown",  rows.stream().map(this::toLanguageRow).toList());
        return result;
    }

    // =========================================================================
    // Country analytics
    // =========================================================================

    public Map<String, Object> getCountryAnalytics(
            String countryCode, LocalDate start, LocalDate end) {

        LocalDate from = start != null ? start : today().minusDays(30);
        LocalDate to   = end   != null ? end   : today();

        List<CultureEngagementDaily> engagement =
                engagementRepo.findByCountryCodeAndAggregationDateBetween(countryCode, from, to);
        List<CultureContributionDaily> contributions =
                contributionRepo.findByCountryCodeAndAggregationDateBetween(countryCode, from, to);

        long totalViews   = engagement.stream().mapToLong(e ->
                e.getContentViews() != null ? e.getContentViews() : 0L).sum();
        long totalLikes   = engagement.stream().mapToLong(e ->
                e.getLikes()        != null ? e.getLikes()        : 0L).sum();
        long totalContribs= contributions.stream().mapToLong(c ->
                c.getTotalContributions() != null ? c.getTotalContributions() : 0L).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("countryCode",          countryCode);
        result.put("period",               Map.of("from", from, "to", to));
        result.put("totalContentViews",    totalViews);
        result.put("totalLikes",           totalLikes);
        result.put("totalContributions",   totalContribs);
        result.put("engagementBreakdown",  engagement.stream().map(this::toEngagementRow).toList());
        result.put("contributionBreakdown",contributions.stream().map(this::toContributionRow).toList());
        return result;
    }

    // =========================================================================
    // Artisan analytics
    // =========================================================================

    public Map<String, Object> getArtisanAnalytics(
            String artisanId, LocalDate start, LocalDate end) {

        LocalDate from = start != null ? start : today().minusDays(30);
        LocalDate to   = end   != null ? end   : today();

        List<ArtisanKpisDaily> rows =
                artisanRepo.findByArtisanIdAndAggregationDateBetweenOrderByAggregationDateDesc(
                        artisanId, from, to);

        long totalSales   = rows.stream().mapToLong(r -> r.getSales()        != null ? r.getSales()        : 0L).sum();
        long totalViews   = rows.stream().mapToLong(r -> r.getTotalViews()   != null ? r.getTotalViews()   : 0L).sum();
        long totalFollowers= rows.stream().mapToLong(r -> r.getNewFollowers()!= null ? r.getNewFollowers() : 0L).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("artisanId",        artisanId);
        result.put("period",           Map.of("from", from, "to", to));
        result.put("totalSales",       totalSales);
        result.put("totalViews",       totalViews);
        result.put("newFollowers",     totalFollowers);
        result.put("dailyKpis",        rows.stream().map(this::toArtisanRow).toList());
        return result;
    }

    /** Partner-facing report: empty source data remains null, never fabricated as zero. */
    public Map<String, Object> getArtisanAnalyticsForPeriod(String artisanId, int periodDays) {
        if (periodDays != 7 && periodDays != 30 && periodDays != 90) {
            throw new IllegalArgumentException("periodDays must be 7, 30 or 90");
        }
        LocalDate to = today();
        LocalDate from = to.minusDays(periodDays - 1L);
        List<ArtisanKpisDaily> rows = artisanRepo.findByArtisanIdAndAggregationDateBetweenOrderByAggregationDateDesc(artisanId, from, to);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("artisanId", artisanId);
        result.put("period", Map.of("days", periodDays, "from", from, "to", to));
        result.put("dataAvailable", !rows.isEmpty());
        result.put("totalSales", rows.isEmpty() ? null : rows.stream().mapToLong(value -> value.getSales() == null ? 0L : value.getSales()).sum());
        result.put("revenue", rows.isEmpty() || rows.stream().allMatch(value -> value.getRevenue() == null) ? null : rows.stream().map(ArtisanKpisDaily::getRevenue).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        result.put("totalViews", rows.isEmpty() ? null : rows.stream().mapToLong(value -> value.getTotalViews() == null ? 0L : value.getTotalViews()).sum());
        result.put("newFollowers", rows.isEmpty() ? null : rows.stream().mapToLong(value -> value.getNewFollowers() == null ? 0L : value.getNewFollowers()).sum());
        result.put("dailyKpis", rows.stream().map(this::toArtisanRow).toList());
        return result;
    }

    // =========================================================================
    // Artwork analytics
    // =========================================================================

    public Map<String, Object> getArtworkAnalytics(
            String artworkId, LocalDate start, LocalDate end) {

        LocalDate from = start != null ? start : today().minusDays(30);
        LocalDate to   = end   != null ? end   : today();

        List<ArtworkPopularityDaily> rows =
                artworkRepo.findByArtworkIdAndAggregationDateBetweenOrderByAggregationDateDesc(
                        artworkId, from, to);

        long totalViews  = rows.stream().mapToLong(r -> r.getViews()     != null ? r.getViews()     : 0L).sum();
        long totalLikes  = rows.stream().mapToLong(r -> r.getLikes()     != null ? r.getLikes()     : 0L).sum();
        long totalShares = rows.stream().mapToLong(r -> r.getShares()    != null ? r.getShares()    : 0L).sum();
        long totalPurchases = rows.stream().mapToLong(r -> r.getPurchases()!= null ? r.getPurchases(): 0L).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("artworkId",      artworkId);
        result.put("period",         Map.of("from", from, "to", to));
        result.put("totalViews",     totalViews);
        result.put("totalLikes",     totalLikes);
        result.put("totalShares",    totalShares);
        result.put("totalPurchases", totalPurchases);
        result.put("dailyStats",     rows.stream().map(this::toArtworkRow).toList());
        return result;
    }

    // =========================================================================
    // Contribution analytics
    // =========================================================================

    public Map<String, Object> getContributionAnalytics(
            String contributionType, String countryCode,
            LocalDate start, LocalDate end) {

        LocalDate from = start != null ? start : today().minusDays(30);
        LocalDate to   = end   != null ? end   : today();

        List<CultureContributionDaily> rows;
        if (countryCode != null && !countryCode.isBlank()) {
            rows = contributionRepo.findByCountryCodeAndAggregationDateBetween(countryCode, from, to);
        } else {
            rows = contributionRepo.findByAggregationDateBetweenOrderByAggregationDateDesc(from, to);
        }

        if (contributionType != null && !contributionType.isBlank()) {
            String type = contributionType.toUpperCase();
            rows = rows.stream()
                    .filter(r -> type.equals(r.getContributionType()))
                    .toList();
        }

        long totalContribs  = rows.stream().mapToLong(r -> r.getTotalContributions()    != null ? r.getTotalContributions()    : 0L).sum();
        long totalVerified  = rows.stream().mapToLong(r -> r.getVerifiedContributions() != null ? r.getVerifiedContributions() : 0L).sum();
        long totalUnique    = rows.stream().mapToLong(r -> r.getUniqueContributors()    != null ? r.getUniqueContributors()    : 0L).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period",             Map.of("from", from, "to", to));
        result.put("countryCode",        countryCode);
        result.put("contributionType",   contributionType);
        result.put("totalContributions", totalContribs);
        result.put("verifiedContributions", totalVerified);
        result.put("uniqueContributors", totalUnique);
        result.put("breakdown",          rows.stream().map(this::toContributionRow).toList());
        return result;
    }

    // =========================================================================
    // Trending
    // =========================================================================

    public Map<String, Object> getTrendingContent(
            String countryCode, int days, int limit) {

        LocalDate from = today().minusDays(Math.max(1, days));
        LocalDate to   = today();

        List<ArtworkPopularityDaily> top = artworkRepo.findTopByDateRange(from, to)
                .stream().limit(Math.min(50, limit)).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period",          Map.of("from", from, "to", to));
        result.put("countryCode",     countryCode);
        result.put("trendingArtworks",top.stream().map(this::toArtworkRow).toList());
        return result;
    }

    // =========================================================================
    // Upsert helpers (called by event consumer)
    // =========================================================================

    @Transactional
    public void upsertLanguageLearning(LocalDate date, String languageCode,
                                       String countryCode, long lessonsCompleted,
                                       long wordsLearned) {
        LanguageLearningDaily row = languageRepo
                .findByAggregationDateAndLanguageCodeAndCountryCode(date, languageCode, countryCode)
                .orElse(LanguageLearningDaily.builder()
                        .aggregationDate(date)
                        .languageCode(languageCode)
                        .countryCode(countryCode)
                        .createdAt(java.time.Instant.now())
                        .build());

        row.setLessonsCompleted((row.getLessonsCompleted() != null ? row.getLessonsCompleted() : 0L) + lessonsCompleted);
        row.setWordsLearned((row.getWordsLearned() != null ? row.getWordsLearned() : 0L) + wordsLearned);
        row.setActiveLearners((row.getActiveLearners() != null ? row.getActiveLearners() : 0L) + 1L);
        languageRepo.save(row);
    }

    @Transactional
    public void upsertArtworkView(LocalDate date, String artworkId, String artisanId) {
        ArtworkPopularityDaily row = artworkRepo
                .findByAggregationDateAndArtworkId(date, artworkId)
                .orElse(ArtworkPopularityDaily.builder()
                        .aggregationDate(date)
                        .artworkId(artworkId)
                        .artisanId(artisanId)
                        .createdAt(java.time.Instant.now())
                        .build());

        row.setViews((row.getViews() != null ? row.getViews() : 0L) + 1L);
        artworkRepo.save(row);
    }

    @Transactional
    public void upsertContribution(LocalDate date, String contributionType,
                                   String countryCode, boolean verified) {
        CultureContributionDaily row = contributionRepo
                .findByAggregationDateAndCountryCodeAndContributionType(date, countryCode, contributionType)
                .orElse(CultureContributionDaily.builder()
                        .aggregationDate(date)
                        .countryCode(countryCode)
                        .contributionType(contributionType.toUpperCase())
                        .createdAt(java.time.Instant.now())
                        .build());

        row.setTotalContributions((row.getTotalContributions() != null ? row.getTotalContributions() : 0L) + 1L);
        if (verified) {
            row.setVerifiedContributions((row.getVerifiedContributions() != null ? row.getVerifiedContributions() : 0L) + 1L);
        }
        contributionRepo.save(row);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    private CultureOverviewResponse.PeriodMetrics periodMetrics(LocalDate from, LocalDate to) {
        Long views = engagementRepo.sumContentViews(from, to, null);
        return CultureOverviewResponse.PeriodMetrics.builder()
                .totalViews(views != null ? views : 0L)
                .build();
    }

    private List<CultureOverviewResponse.LanguageStat> topLanguages(
            LocalDate from, LocalDate to, int limit) {
        List<Object[]> raw = languageRepo.topLanguagesByLearners(from, to);
        int rank = 1;
        List<CultureOverviewResponse.LanguageStat> result = new ArrayList<>();
        for (Object[] row : raw) {
            if (result.size() >= limit) break;
            result.add(CultureOverviewResponse.LanguageStat.builder()
                    .languageCode((String) row[0])
                    .activeLearners(((Number) row[1]).longValue())
                    .rank(rank++)
                    .build());
        }
        return result;
    }

    private List<CultureOverviewResponse.ArtworkStat> trendingArtworks(
            LocalDate from, LocalDate to, int limit) {
        return artworkRepo.findTopByDateRange(from, to).stream().limit(limit)
                .map(a -> CultureOverviewResponse.ArtworkStat.builder()
                        .artworkId(a.getArtworkId())
                        .artisanId(a.getArtisanId())
                        .views(a.getViews() != null ? a.getViews() : 0L)
                        .likes(a.getLikes() != null ? a.getLikes() : 0L)
                        .purchases(a.getPurchases() != null ? a.getPurchases() : 0L)
                        .build())
                .toList();
    }

    // Row mapping helpers — only aggregate fields, no PII
    private Map<String, Object> toLanguageRow(LanguageLearningDaily r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", r.getAggregationDate());
        m.put("languageCode",      r.getLanguageCode());
        m.put("activeLearners",    r.getActiveLearners());
        m.put("lessonsCompleted",  r.getLessonsCompleted());
        m.put("wordsLearned",      r.getWordsLearned());
        m.put("quizzesTaken",      r.getQuizzesTaken());
        m.put("completionRate",    r.getLessonCompletionRate());
        return m;
    }

    private Map<String, Object> toEngagementRow(CultureEngagementDaily r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date",          r.getAggregationDate());
        m.put("countryCode",   r.getCountryCode());
        m.put("contentViews",  r.getContentViews());
        m.put("likes",         r.getLikes());
        m.put("shares",        r.getShares());
        m.put("completed",     r.getContentCompleted());
        return m;
    }

    private Map<String, Object> toContributionRow(CultureContributionDaily r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date",                 r.getAggregationDate());
        m.put("type",                 r.getContributionType());
        m.put("totalContributions",   r.getTotalContributions());
        m.put("verifiedContributions",r.getVerifiedContributions());
        m.put("uniqueContributors",   r.getUniqueContributors());
        m.put("verificationRate",     r.getVerificationRate());
        return m;
    }

    private Map<String, Object> toArtisanRow(ArtisanKpisDaily r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date",            r.getAggregationDate());
        m.put("sales",           r.getSales());
        m.put("revenue",         r.getRevenue());
        m.put("totalViews",      r.getTotalViews());
        m.put("newFollowers",    r.getNewFollowers());
        m.put("engagementRate",  r.getEngagementRate());
        m.put("reachCountries",  r.getReachCountries());
        return m;
    }

    private Map<String, Object> toArtworkRow(ArtworkPopularityDaily r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date",        r.getAggregationDate());
        m.put("artworkId",   r.getArtworkId());
        m.put("views",       r.getViews());
        m.put("likes",       r.getLikes());
        m.put("shares",      r.getShares());
        m.put("purchases",   r.getPurchases());
        m.put("revenue",     r.getRevenue());
        return m;
    }
}
