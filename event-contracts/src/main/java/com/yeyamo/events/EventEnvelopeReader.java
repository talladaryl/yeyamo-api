package com.yeyamo.events;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public final class EventEnvelopeReader {
    private EventEnvelopeReader() {
    }

    public static UUID eventId(JsonNode event) {
        return UUID.fromString(requiredText(event, "eventId"));
    }

    public static String eventType(JsonNode event) {
        return requiredText(event, "eventType");
    }

    public static int version(JsonNode event) {
        int version = event.path("version").asInt(event.path("eventVersion").asInt(0));
        if (version < 1) {
            throw new IllegalArgumentException("Event version is required");
        }
        return version;
    }

    public static String producer(JsonNode event) {
        return requiredText(event, "producer");
    }

    public static Instant occurredAt(JsonNode event) {
        return Instant.parse(requiredText(event, "occurredAt"));
    }

    public static String correlationId(JsonNode event) {
        return requiredText(event, "correlationId");
    }

    public static String aggregateId(JsonNode event) {
        return requiredText(event, "aggregateId");
    }

    public static JsonNode payload(JsonNode event) {
        JsonNode payload = event.get("payload");
        if (payload == null || payload.isNull()) {
            throw new IllegalArgumentException("payload is required");
        }
        return payload;
    }

    private static String requiredText(JsonNode event, String field) {
        JsonNode value = event.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.asText();
    }
}
