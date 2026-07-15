package com.yeyamo_mobile.api.analytics_service.models;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;

import lombok.Getter;
import lombok.Setter;

@Document(indexName = "kpi_history")
@Getter
@Setter
public class KpiHistory {

    @Id
    private UUID id = UUID.randomUUID();

    private String kpiName;
    private LocalDate statDate;
    private Map<String, Object> kpiValue = new LinkedHashMap<>();
    private String eventType;
    private LocalDateTime calculatedAt = LocalDateTime.now();
    private UUID entityId;
    private String entityType;
    private java.util.Set<UUID> appliedEventIds = new java.util.LinkedHashSet<>();
    @Version
    private Long version;

    public boolean increment(UUID eventId, long delta) {
        if (!appliedEventIds.add(eventId)) {
            return false;
        }
        long current = ((Number) kpiValue.getOrDefault("count", 0L)).longValue();
        kpiValue.put("count", Math.max(0, current + delta));
        calculatedAt = LocalDateTime.now();
        return true;
    }
}
