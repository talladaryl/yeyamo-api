package com.yeyamo_mobile.api.commerce_service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service
public class OutboxService {
    private final CommerceOutboxRepository repo;
    private final ObjectMapper json;

    public OutboxService(CommerceOutboxRepository repo, ObjectMapper json) {
        this.repo = repo;
        this.json = json;
    }

    public void append(String topic, String type, String aggregate,
            String correlation, Map<String, Object> payload) {
        try {
            CommerceOutbox event = new CommerceOutbox();
            event.id = UUID.randomUUID();
            event.targetTopic = topic;
            event.eventType = type;
            event.aggregateId = aggregate;
            event.correlationId = correlation;
            event.occurredAt = Instant.now();
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("eventId", event.id);
            envelope.put("eventType", type);
            envelope.put("eventVersion", 1);
            envelope.put("occurredAt", event.occurredAt);
            envelope.put("producer", "commerce-service");
            envelope.put("aggregateId", aggregate);
            envelope.put("correlationId",
                correlation == null ? event.id.toString() : correlation);
            envelope.put("payload", payload);
            event.payload = json.writeValueAsString(envelope);
            repo.save(event);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
