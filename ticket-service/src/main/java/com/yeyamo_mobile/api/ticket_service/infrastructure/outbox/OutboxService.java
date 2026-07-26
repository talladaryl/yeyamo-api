package com.yeyamo_mobile.api.ticket_service.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class OutboxService {
    
    private static final Logger logger = LoggerFactory.getLogger(OutboxService.class);
    
    private final SpringOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    
    public OutboxService(SpringOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }
    
    @Transactional
    public void publishTicketOrderCreated(String orderId, String partnerId,
            String eventId, String ticketTypeId, int quantity,
            BigDecimal amount, String currency) {
        publishEvent("TicketOrder", orderId, "ticket.order.created", Map.of(
            "orderId", orderId,
            "partnerId", partnerId,
            "eventId", eventId,
            "ticketTypeId", ticketTypeId,
            "quantity", quantity,
            "amount", amount,
            "currency", currency
        ));
    }

    @Transactional
    public void publishPaymentRequested(
            String orderId, 
            String userId, 
            BigDecimal amount, 
            String currency,
            Map<String, Object> metadata) {
        
        Map<String, Object> payload = Map.of(
            "sagaId", orderId,
            "bookingId", orderId,
            "userId", userId,
            "amount", amount,
            "currency", currency,
            "idempotencyKey", "ticket:" + orderId + ":authorize"
        );
        
        publishEvent("TicketOrder", orderId, "payment.authorization.requested", payload);
    }
    
    @Transactional
    public void publishTicketsIssued(
            String orderId,
            String partnerId,
            String eventId,
            List<String> ticketIds,
            BigDecimal amount,
            String currency) {
        
        Map<String, Object> payload = Map.of(
            "orderId", orderId,
            "partnerId", partnerId,
            "eventId", eventId,
            "ticketIds", ticketIds,
            "quantity", ticketIds.size(),
            "amount", amount,
            "currency", currency
        );
        
        publishEvent("TicketOrder", orderId, "ticket.issued", payload);
    }
    
    @Transactional
    public void publishTicketScanned(
            String ticketId, 
            String eventId, 
            String userId,
            String scannerUserId) {
        
        Map<String, Object> payload = Map.of(
            "ticketId", ticketId,
            "eventId", eventId,
            "staffId", hashIdentifier(scannerUserId)
        );
        
        publishEvent("Ticket", ticketId, "ticket.validated", payload);
    }

    @Transactional
    public void publishScanRejected(String eventId, String scannerUserId,
            String result) {
        publishEvent("TicketEvent", eventId, "ticket.scan.rejected", Map.of(
            "eventId", eventId,
            "staffId", hashIdentifier(scannerUserId),
            "reason", result
        ));
    }
    
    @Transactional
    public void publishTicketCancelled(String ticketId, String userId, String reason) {
        Map<String, Object> payload = Map.of(
            "eventType", "TicketCancelled",
            "ticketId", ticketId,
            "userId", userId,
            "reason", reason
        );
        
        publishEvent("Ticket", ticketId, "TicketCancelled", payload);
    }
    
    private void publishEvent(String aggregateType, String aggregateId, String eventType, Map<String, Object> payload) {
        try {
            UUID eventId = UUID.randomUUID();
            Instant occurredAt = Instant.now();
            Map<String,Object> envelope = new LinkedHashMap<>();
            envelope.put("eventId", eventId);
            envelope.put("eventType", eventType);
            envelope.put("eventVersion", 1);
            envelope.put("occurredAt", occurredAt);
            envelope.put("producer", "ticket-service");
            envelope.put("aggregateId", aggregateId);
            envelope.put("correlationId", eventId.toString());
            envelope.put("payload", payload);
            TicketOutboxEntity outbox = new TicketOutboxEntity();
            outbox.setId(eventId);
            outbox.setAggregateType(aggregateType);
            outbox.setAggregateId(aggregateId);
            outbox.setEventType(eventType);
            outbox.setPayload(objectMapper.writeValueAsString(envelope));
            outbox.setStatus("PENDING");
            
            outboxRepository.save(outbox);
            
            logger.debug("Published {} event to outbox for {}: {}", eventType, aggregateType, aggregateId);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event payload", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    private String hashIdentifier(String value) {
        if (value == null) return "anonymous";
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
