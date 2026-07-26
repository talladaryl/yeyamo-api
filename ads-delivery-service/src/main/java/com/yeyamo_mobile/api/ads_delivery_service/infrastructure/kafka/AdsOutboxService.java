package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class AdsOutboxService {
    private final AdsOutboxRepository repository;
    private final ObjectMapper json;

    public AdsOutboxService(AdsOutboxRepository repository, ObjectMapper json) {
        this.repository = repository;
        this.json = json;
    }

    public void append(String type, String campaignId, String correlationId,
            Map<String, Object> payload) {
        try {
            AdsOutboxEvent event = new AdsOutboxEvent();
            event.id = UUID.randomUUID();
            event.aggregateId = campaignId;
            event.eventType = type;
            event.occurredAt = Instant.now();
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("eventId", event.id);
            envelope.put("eventType", type);
            envelope.put("eventVersion", 1);
            envelope.put("occurredAt", event.occurredAt);
            envelope.put("producer", "ads-delivery-service");
            envelope.put("aggregateId", campaignId);
            envelope.put("correlationId", correlationId);
            envelope.put("payload", payload);
            event.payload = json.writeValueAsString(envelope);
            repository.save(event);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot append ads outbox event", exception);
        }
    }

    public String anonymousSubject(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))).substring(0, 24);
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
