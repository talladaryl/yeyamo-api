package com.yeyamo_mobile.api.ticket_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a ticket purchase order.
 * Manages the order lifecycle from creation through payment to ticket issuance.
 */
@Entity
@Table(name = "ticket_orders", indexes = {
    @Index(name = "idx_order_reference", columnList = "reference", unique = true),
    @Index(name = "idx_order_user", columnList = "user_id"),
    @Index(name = "idx_order_event", columnList = "event_id"),
    @Index(name = "idx_order_partner", columnList = "partner_id"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @NotNull
    @Size(min = 10, max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String reference;
    
    @NotNull
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @NotNull
    @Column(name = "partner_id", nullable = false)
    private String partnerId;
    
    @NotNull
    @Column(name = "event_id", nullable = false)
    private String eventId;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TicketOrderStatus status = TicketOrderStatus.CREATED;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    @Builder.Default
    private TicketOrderStatus paymentStatus = TicketOrderStatus.AWAITING_PAYMENT;
    
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
    
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "service_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal serviceFee = BigDecimal.ZERO;
    
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    
    @NotNull
    @Size(min = 3, max = 3)
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Column(name = "promotion_id")
    private String promotionId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    /**
     * Check if order has expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Calculate total amount from components
     */
    public void calculateTotal() {
        this.totalAmount = subtotal.subtract(discountAmount).add(serviceFee);
    }
    
    /**
     * Mark order as paid
     */
    public void markAsPaid() {
        if (this.status != TicketOrderStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException("Order is not awaiting payment");
        }
        this.status = TicketOrderStatus.PAID;
        this.paymentStatus = TicketOrderStatus.PAID;
    }
    
    /**
     * Mark order as issued (tickets generated)
     */
    public void markAsIssued() {
        if (this.status != TicketOrderStatus.PAID) {
            throw new IllegalStateException("Order must be paid before issuing tickets");
        }
        this.status = TicketOrderStatus.ISSUED;
    }
    
    @PrePersist
    @PreUpdate
    private void validateAmounts() {
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Subtotal cannot be negative");
        }
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Total amount cannot be negative");
        }
    }
}
