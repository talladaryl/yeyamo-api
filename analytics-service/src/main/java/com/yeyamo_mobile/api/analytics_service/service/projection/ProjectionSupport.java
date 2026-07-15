package com.yeyamo_mobile.api.analytics_service.service.projection;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.yeyamo_mobile.api.analytics_service.event.AnalyticsDomainEvent;

final class ProjectionSupport {
    private ProjectionSupport() {
    }

    static UUID id(String namespace, Object entityId, LocalDate date) {
        String value = namespace + ":" + entityId + ":" + date;
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    static LocalDate date(AnalyticsDomainEvent event) {
        return event.occurredAt().atZone(ZoneOffset.UTC).toLocalDate();
    }

    static String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node == null ? null : node.get(field);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    static UUID uuid(JsonNode node, String... fields) {
        String value = text(node, fields);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return BigDecimal.ZERO;
        }
        try {
            return value.decimalValue();
        } catch (RuntimeException ignored) {
            return BigDecimal.ZERO;
        }
    }

    static int direction(String eventType, String added, String removed) {
        if (eventType.equals(added)) {
            return 1;
        }
        if (eventType.equals(removed)) {
            return -1;
        }
        return 0;
    }
}
