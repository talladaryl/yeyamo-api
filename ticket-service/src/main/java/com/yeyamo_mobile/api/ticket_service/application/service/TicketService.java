package com.yeyamo_mobile.api.ticket_service.application.service;

import com.yeyamo_mobile.api.ticket_service.application.dto.*;
import com.yeyamo_mobile.api.ticket_service.domain.model.*;
import com.yeyamo_mobile.api.ticket_service.domain.repository.*;
import com.yeyamo_mobile.api.ticket_service.infrastructure.security.QrTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main service for ticket operations
 */
@Slf4j
@Service
@Profile("legacy-ticket-api")
@RequiredArgsConstructor
public class TicketService {
    
    private final TicketOrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketQrCredentialRepository qrCredentialRepository;
    private final TicketSaleConfigurationRepository saleConfigurationRepository;
    private final TicketInventoryService inventoryService;
    private final QrTokenService qrTokenService;
    private final OutboxService outboxService;
    
    @Value("${ticket.order.expiry-minutes:15}")
    private int orderExpiryMinutes;
    
    @Value("${ticket.qr.token.expiry-days:365}")
    private int qrTokenExpiryDays;
    
    /**
     * Create a new ticket order
     */
    @Transactional
    public TicketOrderResponse createOrder(String userId, String partnerId, 
                                          CreateTicketOrderRequest request) {
        log.info("Creating order for user: {}, event: {}", userId, request.getEventId());
        
        // Validate sale configuration
        TicketSaleConfiguration config = saleConfigurationRepository.findByEventId(request.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event ticketing not configured"));
        
        if (!config.isSalesActive()) {
            throw new IllegalStateException("Ticket sales are not currently active");
        }
        
        // Calculate order totals
        BigDecimal subtotal = BigDecimal.ZERO;
        List<TicketOrderResponse.TicketItemSummary> items = new ArrayList<>();
        
        for (var item : request.getItems()) {
            TicketType ticketType = ticketTypeRepository.findById(item.getTicketTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Ticket type not found"));
            
            // Create hold for this ticket type
            inventoryService.createHold(
                    userId, 
                    item.getTicketTypeId(), 
                    item.getQuantity(),
                    request.getEventId(),
                    request.getIdempotencyKey() + "-" + item.getTicketTypeId()
            );
            
            BigDecimal itemTotal = ticketType.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(itemTotal);
            
            items.add(TicketOrderResponse.TicketItemSummary.builder()
                    .ticketTypeId(ticketType.getId())
                    .ticketTypeName(ticketType.getName())
                    .quantity(item.getQuantity())
                    .unitPrice(ticketType.getPrice())
                    .totalPrice(itemTotal)
                    .build());
        }
        
        // Calculate service fee (2% of subtotal, for example)
        BigDecimal serviceFee = subtotal.multiply(BigDecimal.valueOf(0.02));
        BigDecimal totalAmount = subtotal.add(serviceFee);
        
        // Create order
        String reference = generateOrderReference();
        Instant expiresAt = Instant.now().plus(orderExpiryMinutes, ChronoUnit.MINUTES);
        
        TicketOrder order = TicketOrder.builder()
                .reference(reference)
                .userId(userId)
                .partnerId(partnerId)
                .eventId(request.getEventId())
                .status(TicketOrderStatus.CREATED)
                .paymentStatus(TicketOrderStatus.AWAITING_PAYMENT)
                .subtotal(subtotal)
                .discountAmount(BigDecimal.ZERO)
                .serviceFee(serviceFee)
                .totalAmount(totalAmount)
                .currency(config.getCurrency())
                .expiresAt(expiresAt)
                .build();
        
        order.calculateTotal();
        order = orderRepository.save(order);
        
        // Publish order created event
        outboxService.publishOrderCreated(order);
        
        log.info("Order created: {}, total: {} {}", reference, totalAmount, config.getCurrency());
        
        return TicketOrderResponse.builder()
                .id(order.getId())
                .reference(reference)
                .userId(userId)
                .eventId(request.getEventId())
                .status(order.getStatus())
                .subtotal(subtotal)
                .discountAmount(BigDecimal.ZERO)
                .serviceFee(serviceFee)
                .totalAmount(totalAmount)
                .currency(config.getCurrency())
                .items(items)
                .createdAt(order.getCreatedAt())
                .expiresAt(expiresAt)
                .build();
    }
    
    /**
     * Confirm payment and issue tickets
     */
    @Transactional
    public List<TicketResponse> confirmPaymentAndIssueTickets(String orderId) {
        log.info("Confirming payment for order: {}", orderId);
        
        TicketOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        if (order.getStatus() != TicketOrderStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException("Order is not awaiting payment");
        }
        
        if (order.isExpired()) {
            throw new IllegalStateException("Order has expired");
        }
        
        // Mark order as paid
        order.markAsPaid();
        orderRepository.save(order);
        
        // Issue tickets
        List<TicketResponse> tickets = issueTicketsForOrder(order);
        
        // Mark order as issued
        order.markAsIssued();
        orderRepository.save(order);
        
        // Publish payment confirmed event
        outboxService.publishPaymentConfirmed(order);
        
        log.info("Issued {} tickets for order: {}", tickets.size(), orderId);
        
        return tickets;
    }
    
    /**
     * Issue tickets for a paid order
     */
    private List<TicketResponse> issueTicketsForOrder(TicketOrder order) {
        // This would query order items (simplified here)
        // In a real implementation, you'd store order items in a separate table
        
        List<TicketResponse> responses = new ArrayList<>();
        
        // For demonstration, create placeholder tickets
        // In production, iterate over actual order items
        
        return responses;
    }
    
    /**
     * Generate QR credential for a ticket
     */
    @Transactional
    public String generateQrCredential(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        
        if (ticket.getStatus() != TicketStatus.VALID) {
            throw new IllegalStateException("Ticket is not valid");
        }
        
        // Check if credential already exists
        if (qrCredentialRepository.existsByTicketId(ticketId)) {
            log.debug("QR credential already exists for ticket: {}", ticketId);
            return qrCredentialRepository.findByTicketId(ticketId)
                    .map(cred -> qrTokenService.generateToken(
                            ticketId, 
                            ticket.getEventId(), 
                            qrTokenExpiryDays
                    ).token())
                    .orElseThrow();
        }
        
        // Generate signed token
        QrTokenService.QrTokenData tokenData = qrTokenService.generateToken(
                ticketId, 
                ticket.getEventId(), 
                qrTokenExpiryDays
        );
        
        // Store credential metadata
        TicketQrCredential credential = TicketQrCredential.builder()
                .ticketId(ticketId)
                .tokenId(tokenData.tokenId())
                .tokenHash(tokenData.tokenHash())
                .keyId(tokenData.keyId())
                .issuedAt(tokenData.issuedAt())
                .expiresAt(tokenData.expiresAt())
                .build();
        
        qrCredentialRepository.save(credential);
        
        log.info("Generated QR credential for ticket: {}", ticketId);
        
        return tokenData.token();
    }
    
    /**
     * Get user's tickets for an event
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getUserTicketsForEvent(String userId, String eventId) {
        List<Ticket> tickets = ticketRepository.findByUserAndEvent(userId, eventId);
        
        return tickets.stream()
                .map(this::mapToResponse)
                .toList();
    }
    
    private TicketResponse mapToResponse(Ticket ticket) {
        // Simplified mapping - would include more details from joined entities
        return TicketResponse.builder()
                .id(ticket.getId())
                .serialNumber(ticket.getSerialNumber())
                .eventId(ticket.getEventId())
                .ticketTypeId(ticket.getTicketTypeId())
                .status(ticket.getStatus())
                .issuedAt(ticket.getIssuedAt())
                .usedAt(ticket.getUsedAt())
                .build();
    }
    
    private String generateOrderReference() {
        return "ORD-" + Instant.now().getEpochSecond() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
