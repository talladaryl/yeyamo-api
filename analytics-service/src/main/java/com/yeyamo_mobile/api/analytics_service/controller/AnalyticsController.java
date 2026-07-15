package com.yeyamo_mobile.api.analytics_service.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.analytics_service.dto.AnalyticsDashboardResponse;
import com.yeyamo_mobile.api.analytics_service.dto.PlacePopularitySummary;
import com.yeyamo_mobile.api.analytics_service.models.AnalyticsEventLog;
import com.yeyamo_mobile.api.analytics_service.models.KpiHistory;
import com.yeyamo_mobile.api.analytics_service.models.PartnerAnalytics;
import com.yeyamo_mobile.api.analytics_service.models.PlacePopularity;
import com.yeyamo_mobile.api.analytics_service.models.RegionActivity;
import com.yeyamo_mobile.api.analytics_service.models.UserEngagement;
import com.yeyamo_mobile.api.analytics_service.service.AnalyticsQueryService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    private final AnalyticsQueryService service;

    public AnalyticsController(AnalyticsQueryService service) {
        this.service = service;
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Global administration analytics dashboard")
    public AnalyticsDashboardResponse adminDashboard() {
        return service.adminDashboard();
    }

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public List<KpiHistory> latestKpis() {
        return service.latestKpis();
    }

    @GetMapping("/kpis/{kpiName}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public List<KpiHistory> kpiHistory(@PathVariable String kpiName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.kpiHistory(kpiName, from, to);
    }

    @GetMapping("/event-logs")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public List<AnalyticsEventLog> eventLogs() {
        return service.eventLogs();
    }

    @GetMapping("/regions/{regionId}/activity")
    public List<RegionActivity> regionActivity(@PathVariable String regionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.regionActivity(regionId, from, to);
    }

    @GetMapping("/partners/{partnerId}/dashboard")
    @PreAuthorize("@analyticsAccess.canReadPartner(authentication,#partnerId)")
    public List<PartnerAnalytics> partnerDashboard(@PathVariable UUID partnerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.partnerDashboard(partnerId, from, to);
    }

    @GetMapping("/places/popular")
    public List<PlacePopularitySummary> popularPlaces(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return service.popularPlaces(from, to, limit);
    }

    @GetMapping("/places/{placeId}/popularity")
    public List<PlacePopularity> placePopularity(@PathVariable UUID placeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.placePopularity(placeId, from, to);
    }

    @GetMapping("/users/{userId}/engagement")
    @PreAuthorize("@analyticsAccess.canReadUser(authentication,#userId)")
    public List<UserEngagement> userEngagement(@PathVariable String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.userEngagement(userId, from, to);
    }
}
