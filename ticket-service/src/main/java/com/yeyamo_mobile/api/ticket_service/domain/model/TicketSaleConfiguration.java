package com.yeyamo_mobile.api.ticket_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Configuration for ticket sales for a specific event.
 * Created by a partner to enable ticketing for their event.
 */
@Entity
@Table(name = "ticket_sale_configurations", indexes = {
    @Index(name = "idx_sale_config_event", columnList = "event_id"),
    @Index(name = "idx_sale_config_partner", columnList = "partner_id"),
    @Index(name = "idx_sale_config_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketSaleConfiguration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @NotNull
    @Column(name = "event_id", nullable = false)
    private String eventId;
    
    @NotNull
    @Column(name = "partner_id", nullable = false)
    private String partnerId;
    
    @NotNull
    @Column(name = "sales_start_at", nullable = false)
    private Instant salesStartAt;
    
    @NotNull
    @Column(name = "sales_end_at", nullable = false)
    private Instant salesEndAt;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SaleStatus status = SaleStatus.DRAFT;
    
    @NotNull
    @Min(1)
    @Max(20)
    @Column(name = "max_tickets_per_buyer", nullable = false)
    @Builder.Default
    private Integer maxTicketsPerBuyer = 10;
    
    @NotNull
    @Size(min = 3, max = 3)
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "XOF";
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * Validates that sales period is valid
     */
    @PrePersist
    @PreUpdate
    private void validateSalesPeriod() {
        if (salesEndAt.isBefore(salesStartAt)) {
            throw new IllegalStateException("Sales end date must be after start date");
        }
    }
    
    /**
     * Checks if sales are currently active
     */
    public boolean isSalesActive() {
        Instant now = Instant.now();
        return status == SaleStatus.ACTIVE 
            && !now.isBefore(salesStartAt) 
            && now.isBefore(salesEndAt);
    }
}
