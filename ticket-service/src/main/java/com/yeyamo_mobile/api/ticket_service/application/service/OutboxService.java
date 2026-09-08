package com.yeyamo_mobile.api.ticket_service.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.ticket_service.domain.model.*;
import com.yeyamo_mobile.api.ticket_service.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Transactional Outbox Service.
 * Ensures events are published to Kafka exactly once with database transaction guarantees.
 */
@Slf4j
@Service("ticketApplicationOutboxService")
@Profile("legacy-ticket-api")
@RequiredArgsConstructor
public class OutboxService {
    
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Publish order created event
     */
    @Transactional
    public void publishOrderCreated(TicketOrder order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId());
        payload.put("reference", order.getReference());
        payload.put("userId", order.getUserId());
        payload.put("eventId", order.getEventId());
        payload.put("totalAmount", order.getTotalAmount());
        payload.put("currency", order.getCurrency());
        payload.put("status", order.getStatus().name());
        payload.put("createdAt", order.getCreatedAt().toString());
        
        saveOutboxEvent("TicketOrder", order.getId(), "ticket.order.created", payload);
    }
    
    /**
     * Publish payment confirmed event
     */
    @Transactional
    public void publishPaymentConfirmed(TicketOrder order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId());
        payload.put("reference", order.getReference());
        payload.put("userId", order.getUserId());
        payload.put("eventId", order.getEventId());
        payload.put("totalAmount", order.getTotalAmount());
        payload.put("currency", order.getCurrency());
        payload.put("status", order.getStatus().name());
        
        saveOutboxEvent("TicketOrder", order.getId(), "ticket.payment.confirmed", payload);
    }
    
    /**
     * Publish ticket issued event
     */
    @Transactional
    public void publishTicketIssued(Ticket ticket) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ticketId", ticket.getId());
        payload.put("serialNumber", ticket.getSerialNumber());
        payload.put("orderId", ticket.getOrderId());
        payload.put("userId", ticket.getOwnerUserId());
        payload.put("eventId", ticket.getEventId());
        payload.put("ticketTypeId", ticket.getTicketTypeId());
        payload.put("issuedAt", ticket.getIssuedAt().toString());
        
        saveOutboxEvent("Ticket", ticket.getId(), "ticket.issued", payload);
    }
    
    /**
     * Publish ticket used event
     */
    @Transactional
    public void publishTicketUsed(Ticket ticket, TicketScan scan) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ticketId", ticket.getId());
        payload.put("serialNumber", ticket.getSerialNumber());
        payload.put("eventId", ticket.getEventId());
        payload.put("userId", ticket.getOwnerUserId());
        payload.put("usedAt", ticket.getUsedAt().toString());
        payload.put("scanId", scan.getId());
        payload.put("gateId", scan.getGateId());
        payload.put("scannerUserId", scan.getScannerUserId());
        
        saveOutboxEvent("Ticket", ticket.getId(), "ticket.used", payload);
    }
    
    /**
     * Publish ticket cancelled event
     */
    @Transactional
    public void publishTicketCancelled(Ticket ticket) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ticketId", ticket.getId());
        payload.put("serialNumber", ticket.getSerialNumber());
        payload.put("userId", ticket.getOwnerUserId());
        payload.put("eventId", ticket.getEventId());
        payload.put("cancelledAt", ticket.getCancelledAt().toString());
        
        saveOutboxEvent("Ticket", ticket.getId(), "ticket.cancelled", payload);
    }
    
    /**
     * Publish ticket refunded event
     */
    @Transactional
    public void publishTicketRefunded(Ticket ticket) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ticketId", ticket.getId());
        payload.put("serialNumber", ticket.getSerialNumber());
        payload.put("userId", ticket.getOwnerUserId());
        payload.put("eventId", ticket.getEventId());
        payload.put("refundedAt", ticket.getRefundedAt().toString());
        
        saveOutboxEvent("Ticket", ticket.getId(), "ticket.refunded", payload);
    }
    
    /**
     * Save event to outbox table
     */
    private void saveOutboxEvent(String aggregateType, String aggregateId, 
                                 String eventType, Map<String, Object> payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .published(false)
                    .build();
            
            outboxRepository.save(event);
            
            log.debug("Saved outbox event: {} for aggregate: {}/{}", 
                     eventType, aggregateType, aggregateId);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload", e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }
    
    /**
     * Cleanup old published events (run daily)
     */
    @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
    @Transactional
    public void cleanupOldEvents() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        int deleted = outboxRepository.deletePublishedEventsBefore(cutoff);
        log.info("Cleaned up {} old outbox events", deleted);
    }
}
