package com.yeyamo_mobile.api.analytics_service.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        String correlationId,
        Map<String, Object> payload
) {
    public static AuditEvent failedAnalyticsEvent(String eventType, String correlationId, String errorMessage) {
        return new AuditEvent(
                UUID.randomUUID(),
                "analytics.event.failed",
                1,
                Instant.now(),
                "analytics-service",
                correlationId,
                Map.of("sourceEventType", eventType, "errorMessage", errorMessage)
        );
    }
}
