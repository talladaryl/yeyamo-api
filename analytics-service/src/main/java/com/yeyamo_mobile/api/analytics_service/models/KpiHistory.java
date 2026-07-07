package com.yeyamo_mobile.api.analytics_service.models;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.Id;
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
    private Map<String, Object> kpiValue = new LinkedHashMap<>();
    private String eventType;
    private LocalDateTime calculatedAt = LocalDateTime.now();
    private UUID entityId;
    private String entityType;
}
