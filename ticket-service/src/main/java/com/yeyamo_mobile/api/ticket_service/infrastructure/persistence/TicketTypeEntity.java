package com.yeyamo_mobile.api.ticket_service.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_types")
public class TicketTypeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "sale_configuration_id", nullable = false)
    private UUID saleConfigurationId;
    
    @Column(nullable = false, length = 50)
    private String code;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;
    
    @Column(name = "quantity_total", nullable = false)
    private Integer quantityTotal;
    
    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved = 0;
    
    @Column(name = "quantity_sold", nullable = false)
    private Integer quantitySold = 0;
    
    @Column(name = "sales_start_at")
    private Instant salesStartAt;
    
    @Column(name = "sales_end_at")
    private Instant salesEndAt;
    
    @Column(name = "access_zone", length = 100)
    private String accessZone;
    
    @Column(name = "gate_instructions", columnDefinition = "TEXT")
    private String gateInstructions;
    
    @Column(nullable = false, length = 50)
    private String status = "ACTIVE";
    
    @Version
    @Column(nullable = false)
    private Long version = 0L;
    
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
    
    // Business methods
    public boolean canReserve(int quantity) {
        int available = quantityTotal - quantityReserved - quantitySold;
        return available >= quantity && quantity > 0;
    }
    
    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Insufficient inventory to reserve " + quantity + " tickets");
        }
        this.quantityReserved += quantity;
    }
    
    public void releaseReservation(int quantity) {
        if (quantity > this.quantityReserved) {
            throw new IllegalStateException("Cannot release more than reserved");
        }
        this.quantityReserved -= quantity;
    }
    
    public void confirmSale(int quantity) {
        if (quantity > this.quantityReserved) {
            throw new IllegalStateException("Cannot confirm sale of more than reserved");
        }
        this.quantityReserved -= quantity;
        this.quantitySold += quantity;
    }
    
    public int getAvailableQuantity() {
        return quantityTotal - quantityReserved - quantitySold;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getSaleConfigurationId() { return saleConfigurationId; }
    public void setSaleConfigurationId(UUID saleConfigurationId) { 
        this.saleConfigurationId = saleConfigurationId; 
    }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public Integer getQuantityTotal() { return quantityTotal; }
    public void setQuantityTotal(Integer quantityTotal) { this.quantityTotal = quantityTotal; }
    
    public Integer getQuantityReserved() { return quantityReserved; }
    public void setQuantityReserved(Integer quantityReserved) { 
        this.quantityReserved = quantityReserved; 
    }
    
    public Integer getQuantitySold() { return quantitySold; }
    public void setQuantitySold(Integer quantitySold) { this.quantitySold = quantitySold; }
    
    public Instant getSalesStartAt() { return salesStartAt; }
    public void setSalesStartAt(Instant salesStartAt) { this.salesStartAt = salesStartAt; }
    
    public Instant getSalesEndAt() { return salesEndAt; }
    public void setSalesEndAt(Instant salesEndAt) { this.salesEndAt = salesEndAt; }
    
    public String getAccessZone() { return accessZone; }
    public void setAccessZone(String accessZone) { this.accessZone = accessZone; }
    
    public String getGateInstructions() { return gateInstructions; }
    public void setGateInstructions(String gateInstructions) { 
        this.gateInstructions = gateInstructions; 
    }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
