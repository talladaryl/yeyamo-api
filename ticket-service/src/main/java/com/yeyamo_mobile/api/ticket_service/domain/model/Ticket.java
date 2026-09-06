package com.yeyamo_mobile.api.ticket_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Represents an individual ticket issued to a user.
 * Contains no sensitive user data - all validation relies on signed QR tokens.
 */
@Entity
@Table(name = "tickets", indexes = {
    @Index(name = "idx_ticket_order", columnList = "order_id"),
    @Index(name = "idx_ticket_event", columnList = "event_id"),
    @Index(name = "idx_ticket_type", columnList = "ticket_type_id"),
    @Index(name = "idx_ticket_owner", columnList = "owner_user_id"),
    @Index(name = "idx_ticket_serial", columnList = "serial_number", unique = true),
    @Index(name = "idx_ticket_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @NotNull
    @Column(name = "order_id", nullable = false)
    private String orderId;
    
    @NotNull
    @Column(name = "event_id", nullable = false)
    private String eventId;
    
    @NotNull
    @Column(name = "ticket_type_id", nullable = false)
    private String ticketTypeId;
    
    @NotNull
    @Column(name = "owner_user_id", nullable = false)
    private String ownerUserId;
    
    @NotNull
    @Size(min = 10, max = 50)
    @Column(name = "serial_number", nullable = false, unique = true, length = 50)
    private String serialNumber;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.PENDING_PAYMENT;
    
    @Column(name = "issued_at")
    private Instant issuedAt;
    
    @Column(name = "used_at")
    private Instant usedAt;
    
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    
    @Column(name = "refunded_at")
    private Instant refundedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
    
    /**
     * Mark ticket as issued (payment confirmed, QR generated)
     */
    public void markAsIssued() {
        if (this.status != TicketStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Ticket must be pending payment to be issued");
        }
        this.status = TicketStatus.VALID;
        this.issuedAt = Instant.now();
    }
    
    /**
     * Mark ticket as used (successfully scanned at entry)
     */
    public void markAsUsed() {
        if (this.status != TicketStatus.VALID) {
            throw new IllegalStateException("Only valid tickets can be marked as used");
        }
        this.status = TicketStatus.USED;
        this.usedAt = Instant.now();
    }
    
    /**
     * Cancel ticket (before event)
     */
    public void cancel() {
        if (this.status == TicketStatus.USED) {
            throw new IllegalStateException("Cannot cancel used ticket");
        }
        this.status = TicketStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }
    
    /**
     * Refund ticket
     */
    public void refund() {
        if (this.status == TicketStatus.USED) {
            throw new IllegalStateException("Cannot refund used ticket");
        }
        this.status = TicketStatus.REFUNDED;
        this.refundedAt = Instant.now();
    }
    
    /**
     * Check if ticket can be used for entry
     */
    public boolean isUsable() {
        return this.status == TicketStatus.VALID;
    }
}
