package com.yeyamo_mobile.api.analytics_service.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.yeyamo_mobile.api.analytics_service.dto.AnalyticsDashboardResponse;
import com.yeyamo_mobile.api.analytics_service.dto.PlacePopularitySummary;
import com.yeyamo_mobile.api.analytics_service.models.AnalyticsEventLog;
import com.yeyamo_mobile.api.analytics_service.models.KpiHistory;
import com.yeyamo_mobile.api.analytics_service.models.PartnerAnalytics;
import com.yeyamo_mobile.api.analytics_service.models.PlacePopularity;
import com.yeyamo_mobile.api.analytics_service.models.RegionActivity;
import com.yeyamo_mobile.api.analytics_service.models.UserEngagement;
import com.yeyamo_mobile.api.analytics_service.repository.AnalyticsEventLogRepository;
import com.yeyamo_mobile.api.analytics_service.repository.KpiHistoryRepository;
import com.yeyamo_mobile.api.analytics_service.repository.PartnerAnalyticsRepository;
import com.yeyamo_mobile.api.analytics_service.repository.PlacePopularityRepository;
import com.yeyamo_mobile.api.analytics_service.repository.RegionActivityRepository;
import com.yeyamo_mobile.api.analytics_service.repository.UserEngagementRepository;

@Service
public class AnalyticsQueryService {
    private final int maxRangeDays;
    private final KpiHistoryRepository kpis;
    private final AnalyticsEventLogRepository eventLogs;
    private final RegionActivityRepository regions;
    private final PartnerAnalyticsRepository partners;
    private final PlacePopularityRepository places;
    private final UserEngagementRepository users;

    public AnalyticsQueryService(KpiHistoryRepository kpis, AnalyticsEventLogRepository eventLogs,
            RegionActivityRepository regions, PartnerAnalyticsRepository partners,
            PlacePopularityRepository places, UserEngagementRepository users,
            @Value("${analytics.query.max-range-days:366}") int maxRangeDays) {
        this.kpis = kpis;
        this.eventLogs = eventLogs;
        this.regions = regions;
        this.partners = partners;
        this.places = places;
        this.users = users;
        this.maxRangeDays = maxRangeDays;
    }

    public AnalyticsDashboardResponse adminDashboard() {
        LocalDate today = LocalDate.now();
        return new AnalyticsDashboardResponse(kpis.findTop20ByOrderByCalculatedAtDesc(),
                popularPlaces(today.minusDays(29), today, 20));
    }

    public List<KpiHistory> latestKpis() {
        return kpis.findTop20ByOrderByCalculatedAtDesc();
    }

    public List<KpiHistory> kpiHistory(String kpiName, LocalDate from, LocalDate to) {
        DateRange range = range(from, to, 30);
        return kpis.findByKpiNameAndStatDateBetweenOrderByStatDateDesc(kpiName, range.from(), range.to());
    }

    public List<AnalyticsEventLog> eventLogs() {
        return eventLogs.findTop50ByOrderByProcessedAtDesc();
    }

    public List<RegionActivity> regionActivity(String regionId, LocalDate from, LocalDate to) {
        DateRange range = range(from, to, 30);
        return regions.findByRegionIdAndActivityDateBetweenOrderByActivityDateDesc(
                regionId, range.from(), range.to());
    }

    public List<PartnerAnalytics> partnerDashboard(UUID partnerId, LocalDate from, LocalDate to) {
        DateRange range = range(from, to, 30);
        return partners.findByPartnerIdAndStatDateBetweenOrderByStatDateDesc(partnerId, range.from(), range.to());
    }

    public List<PlacePopularitySummary> popularPlaces(LocalDate from, LocalDate to, int limit) {
        DateRange range = range(from, to, 30);
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        Map<UUID, MutablePopularity> totals = new LinkedHashMap<>();
        for (PlacePopularity place : places.findByStatDateBetween(range.from(), range.to())) {
            totals.computeIfAbsent(place.getPlaceId(), ignored -> new MutablePopularity()).add(place);
        }
        return totals.entrySet().stream()
                .map(entry -> entry.getValue().summary(entry.getKey()))
                .sorted(Comparator.comparing(PlacePopularitySummary::popularityScore).reversed()
                        .thenComparing(PlacePopularitySummary::placeId))
                .limit(boundedLimit)
                .toList();
    }

    public List<PlacePopularity> placePopularity(UUID placeId, LocalDate from, LocalDate to) {
        DateRange range = range(from, to, 30);
        return places.findByPlaceIdAndStatDateBetweenOrderByStatDateDesc(placeId, range.from(), range.to());
    }

    public List<UserEngagement> userEngagement(String userId, LocalDate from, LocalDate to) {
        DateRange range = range(from, to, 30);
        return users.findByUserIdAndEngagementDateBetweenOrderByEngagementDateDesc(
                userId, range.from(), range.to());
    }

    private DateRange range(LocalDate from, LocalDate to, int defaultDays) {
        LocalDate resolvedTo = to == null ? LocalDate.now() : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(defaultDays - 1L) : from;
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
        if (ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) >= maxRangeDays) {
            throw new IllegalArgumentException("Analytics range cannot exceed " + maxRangeDays + " days");
        }
        return new DateRange(resolvedFrom, resolvedTo);
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }

    private static final class MutablePopularity {
        private BigDecimal score = BigDecimal.ZERO;
        private final List<Integer> values = new ArrayList<>(java.util.Collections.nCopies(7, 0));

        void add(PlacePopularity place) {
            score = score.add(place.getPopularityScore());
            add(0, place.getViews());
            add(1, place.getLikes());
            add(2, place.getComments());
            add(3, place.getShares());
            add(4, place.getCheckIns());
            add(5, place.getBookings());
            add(6, place.getPosts());
        }

        private void add(int index, Integer value) {
            values.set(index, values.get(index) + (value == null ? 0 : value));
        }

        PlacePopularitySummary summary(UUID placeId) {
            return new PlacePopularitySummary(placeId, score, values.get(0), values.get(1), values.get(2),
                    values.get(3), values.get(4), values.get(5), values.get(6));
        }
    }
}
