package com.yeyamo_mobile.api.commerce_service.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.commerce_service.application.ArtworkCommerceService;
import com.yeyamo_mobile.api.commerce_service.application.CommerceService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentEventConsumer {
    private final CommerceService service;
    private final ArtworkCommerceService artworks;
    private final ObjectMapper json;
    private final ProcessedPaymentEventRepository processed;

    public PaymentEventConsumer(CommerceService service, ArtworkCommerceService artworks, ObjectMapper json,
            ProcessedPaymentEventRepository processed) {
        this.service = service;
        this.artworks = artworks;
        this.json = json;
        this.processed = processed;
    }

    @KafkaListener(topics = "${yeyamo.kafka.topics.payment-events:payment.events}",
            groupId = "${spring.kafka.consumer.group-id:commerce-service}")
    @Transactional
    public void consume(String raw) throws Exception {
        JsonNode root = json.readTree(raw);
        String eventId = root.path("eventId").asText();
        if (eventId.isBlank() || processed.existsById(eventId)) return;
        if (root.path("eventVersion").asInt() != 1) throw new IllegalArgumentException("Unsupported event version");
        String type = root.path("eventType").asText();
        String correlationId = root.path("correlationId").asText(eventId);
        JsonNode payload = root.path("payload");
        UUID orderId = UUID.fromString(payload.path("bookingId").asText());
        MDC.put("correlationId", correlationId);
        try {
            if ("payment.authorized".equals(type) || "payment.confirmed".equals(type)) {
                service.paid(orderId, payload.path("paymentId").asText());
                artworks.paymentSucceeded(orderId, correlationId);
            } else if ("payment.refunded".equals(type)) {
                service.refundCompleted(orderId, correlationUuid(correlationId), payload.path("refundId").asText());
                artworks.refunded(orderId, correlationId);
            } else if ("payment.failed".equals(type)) {
                service.paymentFailed(orderId);
                artworks.paymentFailed(orderId, correlationId);
            }
            processed.save(new ProcessedPaymentEvent(eventId, type));
        } finally {
            MDC.remove("correlationId");
        }
    }

    private UUID correlationUuid(String correlationId) {
        try {
            return UUID.fromString(correlationId);
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes(correlationId.getBytes(StandardCharsets.UTF_8));
        }
    }
}
