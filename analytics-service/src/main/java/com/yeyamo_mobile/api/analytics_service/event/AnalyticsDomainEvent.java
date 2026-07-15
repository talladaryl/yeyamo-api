package com.yeyamo_mobile.api.analytics_service.event;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public record AnalyticsDomainEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        String correlationId,
        String actorId,
        JsonNode payload
) {
}
