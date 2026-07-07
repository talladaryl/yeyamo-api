package com.yeyamo_mobile.api.analytics_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.yeyamo_mobile.api.analytics_service.dto.AnalyticsDashboardResponse;
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

    private final KpiHistoryRepository kpiHistoryRepository;
    private final AnalyticsEventLogRepository eventLogRepository;
    private final RegionActivityRepository regionActivityRepository;
    private final PartnerAnalyticsRepository partnerAnalyticsRepository;
    private final PlacePopularityRepository placePopularityRepository;
    private final UserEngagementRepository userEngagementRepository;

    public AnalyticsQueryService(
            KpiHistoryRepository kpiHistoryRepository,
            AnalyticsEventLogRepository eventLogRepository,
            RegionActivityRepository regionActivityRepository,
            PartnerAnalyticsRepository partnerAnalyticsRepository,
            PlacePopularityRepository placePopularityRepository,
            UserEngagementRepository userEngagementRepository
    ) {
        this.kpiHistoryRepository = kpiHistoryRepository;
        this.eventLogRepository = eventLogRepository;
        this.regionActivityRepository = regionActivityRepository;
        this.partnerAnalyticsRepository = partnerAnalyticsRepository;
        this.placePopularityRepository = placePopularityRepository;
        this.userEngagementRepository = userEngagementRepository;
    }

    public AnalyticsDashboardResponse adminDashboard() {
        return new AnalyticsDashboardResponse(
                kpiHistoryRepository.findTop20ByOrderByCalculatedAtDesc(),
                placePopularityRepository.findTop20ByOrderByPopularityScoreDesc()
        );
    }

    public List<KpiHistory> latestKpis() {
        return kpiHistoryRepository.findTop20ByOrderByCalculatedAtDesc();
    }

    public List<KpiHistory> kpiHistory(String kpiName) {
        return kpiHistoryRepository.findByKpiNameOrderByCalculatedAtDesc(kpiName);
    }

    public List<AnalyticsEventLog> eventLogs() {
        return eventLogRepository.findTop50ByOrderByProcessedAtDesc();
    }

    public List<RegionActivity> regionActivity(UUID regionId) {
        return regionActivityRepository.findByRegionIdOrderByActivityDateDesc(regionId);
    }

    public List<PartnerAnalytics> partnerDashboard(UUID partnerId) {
        return partnerAnalyticsRepository.findByPartnerIdOrderByStatDateDesc(partnerId);
    }

    public List<PlacePopularity> popularPlaces() {
        return placePopularityRepository.findTop20ByOrderByPopularityScoreDesc();
    }

    public List<PlacePopularity> placePopularity(UUID placeId) {
        return placePopularityRepository.findByPlaceId(placeId);
    }

    public List<UserEngagement> userEngagement(UUID userId) {
        return userEngagementRepository.findByUserIdOrderByEngagementDateDesc(userId);
    }
}
