package com.yeyamo_mobile.api.analytics_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.analytics_service.dto.AnalyticsDashboardResponse;
import com.yeyamo_mobile.api.analytics_service.models.AnalyticsEventLog;
import com.yeyamo_mobile.api.analytics_service.models.KpiHistory;
import com.yeyamo_mobile.api.analytics_service.models.PartnerAnalytics;
import com.yeyamo_mobile.api.analytics_service.models.PlacePopularity;
import com.yeyamo_mobile.api.analytics_service.models.RegionActivity;
import com.yeyamo_mobile.api.analytics_service.models.UserEngagement;
import com.yeyamo_mobile.api.analytics_service.service.AnalyticsQueryService;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsQueryService service;

    public AnalyticsController(AnalyticsQueryService service) {
        this.service = service;
    }

    @GetMapping("/admin/dashboard")
    public AnalyticsDashboardResponse adminDashboard() {
        return service.adminDashboard();
    }

    @GetMapping("/kpis")
    public List<KpiHistory> latestKpis() {
        return service.latestKpis();
    }

    @GetMapping("/kpis/{kpiName}")
    public List<KpiHistory> kpiHistory(@PathVariable String kpiName) {
        return service.kpiHistory(kpiName);
    }

    @GetMapping("/event-logs")
    public List<AnalyticsEventLog> eventLogs() {
        return service.eventLogs();
    }

    @GetMapping("/regions/{regionId}/activity")
    public List<RegionActivity> regionActivity(@PathVariable UUID regionId) {
        return service.regionActivity(regionId);
    }

    @GetMapping("/partners/{partnerId}/dashboard")
    public List<PartnerAnalytics> partnerDashboard(@PathVariable UUID partnerId) {
        return service.partnerDashboard(partnerId);
    }

    @GetMapping("/places/popular")
    public List<PlacePopularity> popularPlaces() {
        return service.popularPlaces();
    }

    @GetMapping("/places/{placeId}/popularity")
    public List<PlacePopularity> placePopularity(@PathVariable UUID placeId) {
        return service.placePopularity(placeId);
    }

    @GetMapping("/users/{userId}/engagement")
    public List<UserEngagement> userEngagement(@PathVariable UUID userId) {
        return service.userEngagement(userId);
    }
}
