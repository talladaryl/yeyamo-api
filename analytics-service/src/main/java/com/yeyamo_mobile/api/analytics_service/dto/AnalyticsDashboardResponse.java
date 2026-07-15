package com.yeyamo_mobile.api.analytics_service.dto;

import java.util.List;

import com.yeyamo_mobile.api.analytics_service.models.KpiHistory;

public record AnalyticsDashboardResponse(
        List<KpiHistory> latestKpis,
        List<PlacePopularitySummary> popularPlaces
) {
}
