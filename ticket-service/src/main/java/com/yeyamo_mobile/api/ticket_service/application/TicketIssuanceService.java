package com.yeyamo_mobile.api.ticket_service.application;

import com.yeyamo_mobile.api.ticket_service.domain.model.TicketOrderStatus;
import com.yeyamo_mobile.api.ticket_service.domain.model.TicketStatus;
import com.yeyamo_mobile.api.ticket_service.infrastructure.crypto.QrTokenService;
import com.yeyamo_mobile.api.ticket_service.infrastructure.outbox.OutboxService;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TicketIssuanceService {
    
    private static final Logger logger = LoggerFactory.getLogger(TicketIssuanceService.class);
    
    private final SpringTicketRepository ticketRepository;
    private final SpringTicketOrderRepository orderRepository;
    private final SpringTicketHoldRepository holdRepository;
    private final QrTokenService qrTokenService;
    private final OutboxService outboxService;
    
    public TicketIssuanceService(
            SpringTicketRepository ticketRepository,
            SpringTicketOrderRepository orderRepository,
            SpringTicketHoldRepository holdRepository,
            QrTokenService qrTokenService,
            OutboxService outboxService) {
        this.ticketRepository = ticketRepository;
        this.orderRepository = orderRepository;
        this.holdRepository = holdRepository;
        this.qrTokenService = qrTokenService;
        this.outboxService = outboxService;
    }
    
    /**
     * Issue tickets after payment confirmed
     * Idempotent operation
     */
    @Transactional
    public List<TicketEntity> issueTickets(UUID orderId) {
        logger.info("Issuing tickets for order: {}", orderId);
        
        // 1. Load order
        TicketOrderEntity order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        // 2. Check if tickets already issued (idempotency)
        List<TicketEntity> existingTickets = ticketRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        if (!existingTickets.isEmpty()) {
            logger.info("Tickets already issued for order: {}", orderId);
            return existingTickets;
        }
        
        // 3. Validate order status
        if (order.getPaymentStatus() == null || !order.getPaymentStatus().equals("CONFIRMED")) {
            throw new IllegalStateException("Order payment not confirmed: " + order.getPaymentStatus());
        }
        
        if (order.getStatus() == TicketOrderStatus.ISSUED) {
            logger.warn("Order already marked as issued: {}", orderId);
            return existingTickets;
        }
        
        // 4. Get hold to know quantity
        TicketHoldEntity hold = holdRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalStateException("Hold not found for order: " + orderId));
        
        // 5. Issue tickets
        List<TicketEntity> tickets = new ArrayList<>();
        
        for (int i = 0; i < hold.getQuantity(); i++) {
            TicketEntity ticket = new TicketEntity();
            ticket.setOrderId(orderId);
            ticket.setEventId(order.getEventId());
            ticket.setTicketTypeId(hold.getTicketTypeId());
            ticket.setOwnerUserId(order.getUserId());
            ticket.setSerialNumber(generateSerialNumber(order, i));
            ticket.setStatus(TicketStatus.VALID);
            ticket.setIssuedAt(Instant.now());
            
            ticket = ticketRepository.save(ticket);
            
            // Generate QR token
            try {
                String qrToken = qrTokenService.generateQrToken(ticket);
                logger.debug("Generated QR token for ticket: {}", ticket.getId());
            } catch (Exception e) {
                logger.error("Failed to generate QR token for ticket: " + ticket.getId(), e);
                // Continue - QR can be regenerated later if needed
            }
            
            tickets.add(ticket);
        }
        
        // 6. Update order status
        order.setStatus(TicketOrderStatus.ISSUED);
        order.setIssuedAt(Instant.now());
        orderRepository.save(order);
        
        // 7. Publish tickets issued event (via outbox)
        outboxService.publishTicketsIssued(
            orderId.toString(),
            order.getPartnerId(),
            order.getEventId(),
            tickets.stream().map(TicketEntity::getId).map(UUID::toString).toList(),
            order.getTotalAmount(),
            order.getCurrency()
        );
        
        logger.info("Issued {} tickets for order: {}", tickets.size(), orderId);
        
        return tickets;
    }
    
    /**
     * Get ticket with QR code
     */
    @Transactional
    public TicketWithQr getTicketWithQr(UUID ticketId, String userId) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
        
        // Verify ownership
        if (!ticket.getOwnerUserId().equals(userId)) {
            throw new SecurityException("Ticket does not belong to user");
        }
        
        // Generate fresh QR token if needed
        String qrToken = qrTokenService.generateQrToken(ticket);
        
        return new TicketWithQr(ticket, qrToken);
    }
    
    /**
     * Get user tickets
     */
    @Transactional(readOnly = true)
    public List<TicketEntity> getUserTickets(String userId) {
        return ticketRepository.findByOwnerUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get valid tickets for user
     */
    @Transactional(readOnly = true)
    public List<TicketEntity> getValidUserTickets(String userId) {
        return ticketRepository.findByOwnerUserIdAndStatus(userId, TicketStatus.VALID);
    }
    
    /**
     * Cancel ticket (refund scenario)
     */
    @Transactional
    public void cancelTicket(UUID ticketId, String reason) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
        
        if (ticket.isUsed()) {
            throw new IllegalStateException("Cannot cancel used ticket");
        }
        
        ticket.cancel();
        ticketRepository.save(ticket);
        
        // Revoke QR token
        qrTokenService.revokeQrToken(ticketId);
        
        logger.info("Cancelled ticket: {} - Reason: {}", ticketId, reason);
    }
    
    /**
     * Refund ticket
     */
    @Transactional
    public void refundTicket(UUID ticketId) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
        
        ticket.refund();
        ticketRepository.save(ticket);
        
        // Revoke QR token
        qrTokenService.revokeQrToken(ticketId);
        
        logger.info("Refunded ticket: {}", ticketId);
    }
    
    private String generateSerialNumber(TicketOrderEntity order, int index) {
        return String.format("%s-%03d", order.getReference(), index + 1);
    }
    
    // DTOs
    public record TicketWithQr(
        TicketEntity ticket,
        String qrToken
    ) {}
}
