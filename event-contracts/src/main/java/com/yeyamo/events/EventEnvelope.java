package com.yeyamo.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int version,
        String producer,
        Instant occurredAt,
        String correlationId,
        String aggregateId,
        T payload) {

    public EventEnvelope {
        Objects.requireNonNull(eventId, "eventId");
        requireText(eventType, "eventType");
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }
        requireText(producer, "producer");
        Objects.requireNonNull(occurredAt, "occurredAt");
        requireText(correlationId, "correlationId");
        requireText(aggregateId, "aggregateId");
        Objects.requireNonNull(payload, "payload");
    }

    public static <T> EventEnvelope<T> create(
            String eventType,
            String producer,
            String correlationId,
            String aggregateId,
            T payload) {
        UUID eventId = UUID.randomUUID();
        return new EventEnvelope<>(
                eventId,
                eventType,
                1,
                producer,
                Instant.now(),
                correlationId == null || correlationId.isBlank() ? eventId.toString() : correlationId,
                aggregateId,
                payload);
    }

    @JsonProperty("eventVersion")
    public int legacyEventVersion() {
        return version;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
