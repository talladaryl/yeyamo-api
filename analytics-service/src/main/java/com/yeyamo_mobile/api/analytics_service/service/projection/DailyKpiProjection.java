package com.yeyamo_mobile.api.analytics_service.service.projection;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.analytics_service.event.AnalyticsDomainEvent;
import com.yeyamo_mobile.api.analytics_service.models.KpiHistory;
import com.yeyamo_mobile.api.analytics_service.repository.KpiHistoryRepository;

@Component
public class DailyKpiProjection implements AnalyticsProjection {
    private final KpiHistoryRepository kpis;

    public DailyKpiProjection(KpiHistoryRepository kpis) {
        this.kpis = kpis;
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public boolean supports(AnalyticsDomainEvent event) {
        return !event.eventType().startsWith("analytics.");
    }

    @Override
    public void project(AnalyticsDomainEvent event) {
        String kpiName = resolveKpiName(event.eventType());
        LocalDate date = ProjectionSupport.date(event);
        UUID id = ProjectionSupport.id("daily-kpi", kpiName, date);
        KpiHistory projection = kpis.findById(id).orElseGet(KpiHistory::new);
        projection.setId(id);
        projection.setKpiName(kpiName);
        projection.setStatDate(date);
        projection.setEventType(event.eventType());
        projection.setEntityType(resolveEntityType(event.eventType()));
        projection.setEntityId(ProjectionSupport.uuid(event.payload(), "id", "postId", "assetId",
                "placeId", "bookingId", "partnerId", "profileId"));
        if (projection.getKpiValue().isEmpty()) {
            projection.setKpiValue(new java.util.LinkedHashMap<>(Map.of("count", 0L)));
        }
        if (projection.increment(event.eventId(), 1)) {
            kpis.save(projection);
        }
    }

    private String resolveKpiName(String eventType) {
        int separator = eventType.indexOf('.');
        String domain = separator < 0 ? "events" : eventType.substring(0, separator);
        return domain + "_events";
    }

    private String resolveEntityType(String eventType) {
        int separator = eventType.indexOf('.');
        return (separator < 0 ? "EVENT" : eventType.substring(0, separator)).toUpperCase();
    }
}
