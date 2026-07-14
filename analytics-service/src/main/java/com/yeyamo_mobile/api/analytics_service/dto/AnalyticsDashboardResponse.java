package com.yeyamo_mobile.api.analytics_service.dto;

import java.util.List;

import com.yeyamo_mobile.api.analytics_service.models.KpiHistory;
import com.yeyamo_mobile.api.analytics_service.models.PlacePopularity;

public record AnalyticsDashboardResponse(
        List<KpiHistory> latestKpis,
        List<PlacePopularity> popularPlaces
) {
}
