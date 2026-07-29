package com.yeyamo_mobile.api.payment_service.infrastructure.outbox;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo.events.EventEnvelope;
import com.yeyamo_mobile.api.payment_service.application.port.PaymentOutboxPort;

@Component
public class JpaPaymentOutboxAdapter implements PaymentOutboxPort {
    private final PaymentOutboxRepository repository;
    private final ObjectMapper mapper;
    private final String topic;

    public JpaPaymentOutboxAdapter(
            PaymentOutboxRepository repository,
            ObjectMapper mapper,
            @Value("${yeyamo.kafka.topics.payment-events:payment.events}") String topic) {
        this.repository = repository;
        this.mapper = mapper;
        this.topic = topic;
    }

    @Override
    public void append(String type, String aggregateId, String correlationId, Map<String, Object> payload) {
        try {
            var envelope = EventEnvelope.create(
                    type,
                    "payment-service",
                    correlationId,
                    aggregateId,
                    payload);
            var row = new PaymentOutboxEvent();
            row.id = envelope.eventId();
            row.aggregateId = envelope.aggregateId();
            row.eventType = envelope.eventType();
            row.targetTopic = topic;
            row.payload = mapper.writeValueAsString(envelope);
            row.occurredAt = envelope.occurredAt();
            repository.save(row);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create payment outbox event", exception);
        }
    }
}
