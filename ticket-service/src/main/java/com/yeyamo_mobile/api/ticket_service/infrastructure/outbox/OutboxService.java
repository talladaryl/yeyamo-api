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
            String userId, 
            String eventId,
            List<String> ticketIds) {
        
        Map<String, Object> payload = Map.of(
            "eventType", "TicketsIssued",
            "orderId", orderId,
            "userId", userId,
            "eventId", eventId,
            "ticketIds", ticketIds,
            "quantity", ticketIds.size()
        );
        
        publishEvent("TicketOrder", orderId, "TicketsIssued", payload);
    }
    
    @Transactional
    public void publishTicketScanned(
            String ticketId, 
            String eventId, 
            String userId,
            String scannerUserId) {
        
        Map<String, Object> payload = Map.of(
            "eventType", "TicketScanned",
            "ticketId", ticketId,
            "eventId", eventId,
            "userId", userId,
            "scannerUserId", scannerUserId
        );
        
        publishEvent("Ticket", ticketId, "TicketScanned", payload);
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
}
