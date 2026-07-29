package com.yeyamo.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class EventEnvelopeTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void serializesCanonicalAndLegacyVersionsDuringMigration() throws Exception {
        var envelope = EventEnvelope.create(
                "payment.succeeded",
                "payment-service",
                "corr-1",
                "payment-1",
                Map.of("paymentId", "payment-1"));

        var json = mapper.readTree(mapper.writeValueAsBytes(envelope));

        assertEquals(1, json.path("version").asInt());
        assertEquals(1, json.path("eventVersion").asInt());
        assertEquals(1, EventEnvelopeReader.version(json));
    }

    @Test
    void rejectsIncompleteEnvelope() {
        assertThrows(IllegalArgumentException.class,
                () -> EventEnvelope.create("", "payment-service", "corr-1", "payment-1", Map.of()));
    }
}
