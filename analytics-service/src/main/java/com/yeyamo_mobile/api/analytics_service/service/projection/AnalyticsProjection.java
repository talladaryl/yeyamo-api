package com.yeyamo_mobile.api.analytics_service.service.projection;

import com.yeyamo_mobile.api.analytics_service.event.AnalyticsDomainEvent;

public interface AnalyticsProjection {
    int order();
    boolean supports(AnalyticsDomainEvent event);
    void project(AnalyticsDomainEvent event);
}
