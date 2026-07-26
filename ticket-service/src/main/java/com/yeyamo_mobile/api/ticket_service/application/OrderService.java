package com.yeyamo_mobile.api.ticket_service.application;

import com.yeyamo_mobile.api.ticket_service.domain.model.TicketOrderStatus;
import com.yeyamo_mobile.api.ticket_service.infrastructure.outbox.OutboxService;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    private final SpringTicketOrderRepository orderRepository;
    private final SpringTicketHoldRepository holdRepository;
    private final SpringTicketTypeRepository ticketTypeRepository;
    private final SpringTicketSaleConfigurationRepository configRepository;
    private final OutboxService outboxService;
    private final BigDecimal serviceFeePercent;
    
    public OrderService(
            SpringTicketOrderRepository orderRepository,
            SpringTicketHoldRepository holdRepository,
            SpringTicketTypeRepository ticketTypeRepository,
            SpringTicketSaleConfigurationRepository configRepository,
            OutboxService outboxService,
            @Value("${yeyamo.tickets.service-fee-percent:5.0}") BigDecimal serviceFeePercent) {
        this.orderRepository = orderRepository;
        this.holdRepository = holdRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.configRepository = configRepository;
        this.outboxService = outboxService;
        this.serviceFeePercent = serviceFeePercent;
    }
    
    /**
     * Create order from hold
     * Idempotent operation
     */
    @Transactional
    public TicketOrderEntity createOrder(CreateOrderRequest request) {
        logger.info("Creating order for user: {} from hold: {}", request.userId(), request.holdId());
        
        // 1. Load hold
        TicketHoldEntity hold = holdRepository.findById(request.holdId())
            .orElseThrow(() -> new IllegalArgumentException("Hold not found: " + request.holdId()));
        
        // 2. Validate hold belongs to user
        if (!hold.getUserId().equals(request.userId())) {
            throw new SecurityException("Hold does not belong to user");
        }
        
        // 3. Check if order already exists for this hold (idempotency)
        if (hold.getOrderId() != null) {
            Optional<TicketOrderEntity> existing = orderRepository.findById(hold.getOrderId());
            if (existing.isPresent()) {
                logger.info("Order already exists for hold: {}", request.holdId());
                return existing.get();
            }
        }
        
        // 4. Validate hold status
        if (!"ACTIVE".equals(hold.getStatus())) {
            throw new IllegalStateException("Hold is not active: " + hold.getStatus());
        }
        
        if (hold.isExpired()) {
            throw new IllegalStateException("Hold has expired");
        }
        
        // 5. Load ticket type and config
        TicketTypeEntity ticketType = ticketTypeRepository
            .findById(hold.getTicketTypeId())
            .orElseThrow(() -> new IllegalStateException("Ticket type not found"));
        
        TicketSaleConfigurationEntity config = configRepository
            .findById(ticketType.getSaleConfigurationId())
            .orElseThrow(() -> new IllegalStateException("Sale configuration not found"));
        
        // 6. Calculate amounts
        BigDecimal subtotal = ticketType.getPrice()
            .multiply(BigDecimal.valueOf(hold.getQuantity()));
        
        BigDecimal discount = request.promotionCode() != null 
            ? calculateDiscount(subtotal, request.promotionCode())
            : BigDecimal.ZERO;
        
        BigDecimal serviceFee = subtotal
            .subtract(discount)
            .multiply(serviceFeePercent.divide(BigDecimal.valueOf(100)));
        
        BigDecimal total = subtotal
            .subtract(discount)
            .add(serviceFee);
        
        // 7. Create order
        TicketOrderEntity order = new TicketOrderEntity();
        order.setReference(generateOrderReference());
        order.setUserId(request.userId());
        order.setPartnerId(config.getPartnerId());
        order.setEventId(hold.getEventId());
        order.setStatus(TicketOrderStatus.CREATED);
        order.setPaymentStatus("PENDING");
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discount);
        order.setServiceFee(serviceFee);
        order.setTotalAmount(total);
        order.setCurrency(config.getCurrency());
        order.setPromotionId(request.promotionCode());
        order.setExpiresAt(hold.getExpiresAt());
        
        order = orderRepository.save(order);
        
        // 8. Link hold to order
        hold.setOrderId(order.getId());
        hold.setStatus("CONSUMED");
        holdRepository.save(hold);
        
        // 9. Update order status
        order.setStatus(TicketOrderStatus.AWAITING_PAYMENT);
        orderRepository.save(order);
        
        // 10. Publish payment request event (via outbox)
        outboxService.publishPaymentRequested(
            order.getId().toString(),
            order.getUserId(),
            order.getTotalAmount(),
            order.getCurrency(),
            Map.of(
                "orderId", order.getId().toString(),
                "eventId", order.getEventId(),
                "ticketTypeId", ticketType.getId().toString(),
                "quantity", hold.getQuantity()
            )
        );
        
        logger.info("Created order: {} (ref: {}) - Total: {} {}", 
            order.getId(), order.getReference(), order.getTotalAmount(), order.getCurrency());
        
        return order;
    }
    
    /**
     * Cancel order
     */
    @Transactional
    public void cancelOrder(UUID orderId, String reason) {
        TicketOrderEntity order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        if (order.getStatus() == TicketOrderStatus.ISSUED) {
            throw new IllegalStateException("Cannot cancel issued order");
        }
        
        if (order.getStatus() == TicketOrderStatus.CANCELLED) {
            logger.warn("Order already cancelled: {}", orderId);
            return;
        }
        
        order.setStatus(TicketOrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        orderRepository.save(order);
        
        logger.info("Cancelled order: {} - Reason: {}", orderId, reason);
    }
    
    /**
     * Get order by reference
     */
    @Transactional(readOnly = true)
    public Optional<TicketOrderEntity> getOrderByReference(String reference) {
        return orderRepository.findByReference(reference);
    }
    
    /**
     * Get user orders
     */
    @Transactional(readOnly = true)
    public List<TicketOrderEntity> getUserOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get event orders (for partner)
     */
    @Transactional(readOnly = true)
    public List<TicketOrderEntity> getEventOrders(String eventId) {
        return orderRepository.findByEventIdOrderByCreatedAtDesc(eventId);
    }
    
    private String generateOrderReference() {
        long timestamp = Instant.now().getEpochSecond();
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("TKT-%d-%s", timestamp, random);
    }
    
    private BigDecimal calculateDiscount(BigDecimal subtotal, String promotionCode) {
        // TODO: Implement promotion code validation
        // For now, return zero discount
        return BigDecimal.ZERO;
    }
    
    // DTOs
    public record CreateOrderRequest(
        UUID holdId,
        String userId,
        String promotionCode
    ) {}
}
