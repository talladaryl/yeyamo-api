package com.yeyamo_mobile.api.ticket_service.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_sale_configurations")
public class TicketSaleConfigurationEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;
    
    @Column(name = "partner_id", nullable = false)
    private String partnerId;
    
    @Column(name = "sales_start_at", nullable = false)
    private Instant salesStartAt;
    
    @Column(name = "sales_end_at", nullable = false)
    private Instant salesEndAt;
    
    @Column(nullable = false, length = 50)
    private String status;
    
    @Column(name = "max_tickets_per_buyer", nullable = false)
    private Integer maxTicketsPerBuyer = 10;
    
    @Column(nullable = false, length = 3)
    private String currency = "XOF";
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    
    public Instant getSalesStartAt() { return salesStartAt; }
    public void setSalesStartAt(Instant salesStartAt) { this.salesStartAt = salesStartAt; }
    
    public Instant getSalesEndAt() { return salesEndAt; }
    public void setSalesEndAt(Instant salesEndAt) { this.salesEndAt = salesEndAt; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getMaxTicketsPerBuyer() { return maxTicketsPerBuyer; }
    public void setMaxTicketsPerBuyer(Integer maxTicketsPerBuyer) { this.maxTicketsPerBuyer = maxTicketsPerBuyer; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
