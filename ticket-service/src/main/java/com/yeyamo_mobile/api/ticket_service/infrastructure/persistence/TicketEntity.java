package com.yeyamo_mobile.api.ticket_service.infrastructure.persistence;

import com.yeyamo_mobile.api.ticket_service.domain.model.TicketStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class TicketEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    
    @Column(name = "event_id", nullable = false)
    private String eventId;
    
    @Column(name = "ticket_type_id", nullable = false)
    private UUID ticketTypeId;
    
    @Column(name = "owner_user_id", nullable = false)
    private String ownerUserId;
    
    @Column(name = "serial_number", nullable = false, unique = true, length = 100)
    private String serialNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TicketStatus status = TicketStatus.PENDING_PAYMENT;
    
    @Column(name = "issued_at")
    private Instant issuedAt;
    
    @Column(name = "used_at")
    private Instant usedAt;
    
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    
    @Column(name = "refunded_at")
    private Instant refundedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Version
    @Column(nullable = false)
    private Long version = 0L;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
    
    public boolean isValid() {
        return status == TicketStatus.VALID;
    }
    
    public boolean isUsed() {
        return status == TicketStatus.USED;
    }
    
    public void markAsUsed() {
        if (!isValid()) {
            throw new IllegalStateException("Cannot use ticket with status: " + status);
        }
        this.status = TicketStatus.USED;
        this.usedAt = Instant.now();
    }
    
    public void cancel() {
        if (isUsed()) {
            throw new IllegalStateException("Cannot cancel used ticket");
        }
        this.status = TicketStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }
    
    public void refund() {
        this.status = TicketStatus.REFUNDED;
        this.refundedAt = Instant.now();
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public UUID getTicketTypeId() { return ticketTypeId; }
    public void setTicketTypeId(UUID ticketTypeId) { this.ticketTypeId = ticketTypeId; }
    
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
    
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
    
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    
    public Instant getRefundedAt() { return refundedAt; }
    public void setRefundedAt(Instant refundedAt) { this.refundedAt = refundedAt; }
    
    public Instant getCreatedAt() { return createdAt; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
