package com.yeyamo_mobile.api.analytics_service.models;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import com.yeyamo_mobile.api.analytics_service.enums.AnalyticsEventStatus;

import lombok.Getter;
import lombok.Setter;

@Document(indexName = "analytics_event_logs")
@Getter
@Setter
public class AnalyticsEventLog {

    @Id
    private UUID id = UUID.randomUUID();

    private UUID eventId;
    private String eventType;
    private String userId;
    private String service;
    private String correlationId;
    private java.time.Instant occurredAt;
    private LocalDateTime processedAt = LocalDateTime.now();
    private AnalyticsEventStatus status;
}
