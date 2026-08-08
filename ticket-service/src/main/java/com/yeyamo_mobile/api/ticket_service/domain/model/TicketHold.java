package com.yeyamo_mobile.api.ticket_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Temporary hold on ticket inventory during the checkout process.
 * Automatically expires after TTL to release inventory.
 */
@Entity
@Table(name = "ticket_holds", indexes = {
    @Index(name = "idx_hold_user", columnList = "user_id"),
    @Index(name = "idx_hold_event", columnList = "event_id"),
    @Index(name = "idx_hold_status_expires", columnList = "status,expires_at"),
    @Index(name = "idx_hold_idempotency", columnList = "idempotency_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketHold {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @NotNull
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @NotNull
    @Column(name = "event_id", nullable = false)
    private String eventId;
    
    @NotNull
    @Column(name = "ticket_type_id", nullable = false)
    private String ticketTypeId;
    
    @NotNull
    @Min(1)
    @Max(20)
    @Column(nullable = false)
    private Integer quantity;
    
    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private HoldStatus status = HoldStatus.ACTIVE;
    
    /**
     * Idempotency key to prevent duplicate holds
     */
    @NotNull
    @Size(min = 20, max = 100)
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * Check if hold has expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if hold is still active
     */
    public boolean isActive() {
        return status == HoldStatus.ACTIVE && !isExpired();
    }
}
