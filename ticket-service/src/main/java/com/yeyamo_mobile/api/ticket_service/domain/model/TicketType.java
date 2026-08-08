package com.yeyamo_mobile.api.ticket_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a type of ticket available for sale (e.g., VIP, Regular, Student).
 * Manages inventory with strict concurrency controls.
 */
@Entity
@Table(name = "ticket_types", indexes = {
    @Index(name = "idx_ticket_type_config", columnList = "sale_configuration_id"),
    @Index(name = "idx_ticket_type_code", columnList = "sale_configuration_id,code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @NotNull
    @Column(name = "sale_configuration_id", nullable = false)
    private String saleConfigurationId;
    
    @NotNull
    @Size(min = 2, max = 20)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    @Column(nullable = false, length = 20)
    private String code;
    
    @NotNull
    @Size(min = 3, max = 100)
    @Column(nullable = false, length = 100)
    private String name;
    
    @Size(max = 500)
    @Column(length = 500)
    private String description;
    
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    
    @NotNull
    @Min(1)
    @Column(name = "quantity_total", nullable = false)
    private Integer quantityTotal;
    
    @NotNull
    @Min(0)
    @Column(name = "quantity_reserved", nullable = false)
    @Builder.Default
    private Integer quantityReserved = 0;
    
    @NotNull
    @Min(0)
    @Column(name = "quantity_sold", nullable = false)
    @Builder.Default
    private Integer quantitySold = 0;
    
    @Column(name = "sales_start_at")
    private Instant salesStartAt;
    
    @Column(name = "sales_end_at")
    private Instant salesEndAt;
    
    @Size(max = 100)
    @Column(name = "access_zone", length = 100)
    private String accessZone;
    
    @Size(max = 500)
    @Column(name = "gate_instructions", length = 500)
    private String gateInstructions;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SaleStatus status = SaleStatus.DRAFT;
    
    /**
     * Optimistic locking version for inventory management
     */
    @Version
    @Column(nullable = false)
    private Long version;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * Calculate available quantity for purchase
     */
    public int getAvailableQuantity() {
        return quantityTotal - quantityReserved - quantitySold;
    }
    
    /**
     * Check if tickets are available for purchase
     */
    public boolean hasAvailableTickets(int requestedQuantity) {
        return getAvailableQuantity() >= requestedQuantity;
    }
    
    /**
     * Reserve tickets (holds inventory temporarily)
     */
    public void reserveTickets(int quantity) {
        if (!hasAvailableTickets(quantity)) {
            throw new IllegalStateException("Insufficient ticket inventory");
        }
        this.quantityReserved += quantity;
    }
    
    /**
     * Release reserved tickets (e.g., on order expiry)
     */
    public void releaseReservedTickets(int quantity) {
        if (quantity > quantityReserved) {
            throw new IllegalStateException("Cannot release more tickets than reserved");
        }
        this.quantityReserved -= quantity;
    }
    
    /**
     * Confirm sale (convert reservation to sold)
     */
    public void confirmSale(int quantity) {
        if (quantity > quantityReserved) {
            throw new IllegalStateException("Cannot confirm more tickets than reserved");
        }
        this.quantityReserved -= quantity;
        this.quantitySold += quantity;
    }
    
    /**
     * Refund sold tickets
     */
    public void refundTickets(int quantity) {
        if (quantity > quantitySold) {
            throw new IllegalStateException("Cannot refund more tickets than sold");
        }
        this.quantitySold -= quantity;
    }
    
    @PrePersist
    @PreUpdate
    private void validateInventory() {
        if (quantityReserved < 0 || quantitySold < 0) {
            throw new IllegalStateException("Inventory quantities cannot be negative");
        }
        if (quantityReserved + quantitySold > quantityTotal) {
            throw new IllegalStateException("Reserved + sold cannot exceed total inventory");
        }
    }
}
