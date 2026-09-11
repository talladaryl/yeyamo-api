package com.yeyamo_mobile.api.ticket_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.ticket_service.application.InventoryService;
import com.yeyamo_mobile.api.ticket_service.application.TicketIssuanceService;
import com.yeyamo_mobile.api.ticket_service.domain.model.TicketOrderStatus;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class PaymentEventConsumer {
    private final ObjectMapper mapper;
    private final SpringTicketOrderRepository orders;
    private final SpringTicketHoldRepository holds;
    private final TicketIssuanceService issuance;
    private final InventoryService inventory;
    private final TicketProcessedEventRepository processed;

    public PaymentEventConsumer(ObjectMapper mapper, SpringTicketOrderRepository orders,
            SpringTicketHoldRepository holds, TicketIssuanceService issuance,
            InventoryService inventory, TicketProcessedEventRepository processed) {
        this.mapper = mapper;
        this.orders = orders;
        this.holds = holds;
        this.issuance = issuance;
        this.inventory = inventory;
        this.processed = processed;
    }

    @KafkaListener(
        topics = "${yeyamo.kafka.topics.payment-events:payment.events}",
        groupId = "${spring.kafka.consumer.group-id:ticket-service}"
    )
    @Transactional
    public void consume(String raw) throws Exception {
        JsonNode event = mapper.readTree(raw);
        UUID eventId = UUID.fromString(required(event, "eventId"));
        if (processed.existsById(eventId)) return;
        if (event.path("eventVersion").asInt() != 1) {
            throw new IllegalArgumentException("Unsupported payment event version");
        }
        String type = required(event, "eventType");
        JsonNode payload = event.path("payload");
        if (!"TICKET_ORDER".equals(payload.path("sourceType").asText())) {
            processed.save(new TicketProcessedEvent(eventId, type));
            return;
        }
        UUID orderId = UUID.fromString(required(payload, "sourceId"));
        switch (type) {
            case "payment.authorized", "payment.confirmed" ->
                confirm(orderId, payload.path("paymentId").asText(null));
            case "payment.failed" -> fail(orderId);
            default -> { }
        }
        processed.save(new TicketProcessedEvent(eventId, type));
    }

    private void confirm(UUID orderId, String paymentId) {
        TicketOrderEntity order = orders.findById(orderId).orElseThrow();
        if ("CONFIRMED".equals(order.getPaymentStatus())) return;
        TicketHoldEntity hold = holds.findByOrderId(orderId).orElseThrow();
        order.setPaymentStatus("CONFIRMED");
        order.setPaymentReference(paymentId);
        order.setStatus(TicketOrderStatus.PAID);
        order.setPaidAt(Instant.now());
        orders.save(order);
        inventory.confirmSale(hold.getTicketTypeId(), hold.getQuantity());
        issuance.issueTickets(orderId);
    }

    private void fail(UUID orderId) {
        TicketOrderEntity order = orders.findById(orderId).orElseThrow();
        if ("FAILED".equals(order.getPaymentStatus())) return;
        order.setPaymentStatus("FAILED");
        order.setStatus(TicketOrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        orders.save(order);
        inventory.compensateFailedPayment(orderId);
    }

    private String required(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }
}
