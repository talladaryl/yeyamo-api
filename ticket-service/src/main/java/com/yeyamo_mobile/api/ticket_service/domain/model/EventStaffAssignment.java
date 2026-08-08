package com.yeyamo_mobile.api.ticket_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Assigns staff members to an event with specific roles and permissions.
 * Controls who can scan tickets and manage access at event gates.
 */
@Entity
@Table(name = "event_staff_assignments", indexes = {
    @Index(name = "idx_staff_event", columnList = "event_id"),
    @Index(name = "idx_staff_partner", columnList = "partner_id"),
    @Index(name = "idx_staff_user", columnList = "user_id"),
    @Index(name = "idx_staff_status", columnList = "status"),
    @Index(name = "idx_staff_event_user", columnList = "event_id,user_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStaffAssignment {
    
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
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StaffRole role;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StaffStatus status = StaffStatus.ACTIVE;
    
    @NotNull
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
    
    @NotNull
    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * Check if assignment is currently valid
     */
    public boolean isValid() {
        Instant now = Instant.now();
        return status == StaffStatus.ACTIVE 
            && !now.isBefore(validFrom) 
            && now.isBefore(validUntil);
    }
    
    /**
     * Check if staff can scan tickets
     */
    public boolean canScanTickets() {
        return isValid() && (role == StaffRole.ACCESS_CONTROLLER 
            || role == StaffRole.SUPERVISOR 
            || role == StaffRole.EVENT_MANAGER);
    }
}
