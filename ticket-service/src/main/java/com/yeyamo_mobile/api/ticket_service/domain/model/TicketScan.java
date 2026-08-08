package com.yeyamo_mobile.api.ticket_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Audit log of all ticket scan attempts.
 * Records both successful and failed validation attempts for security and analytics.
 */
@Entity
@Table(name = "ticket_scans", indexes = {
    @Index(name = "idx_scan_ticket", columnList = "ticket_id"),
    @Index(name = "idx_scan_event", columnList = "event_id"),
    @Index(name = "idx_scan_scanner", columnList = "scanner_user_id"),
    @Index(name = "idx_scan_result", columnList = "result"),
    @Index(name = "idx_scan_time", columnList = "scanned_at"),
    @Index(name = "idx_scan_gate", columnList = "gate_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketScan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    /**
     * Ticket ID (may be null if scan failed token validation)
     */
    @Column(name = "ticket_id")
    private String ticketId;
    
    @NotNull
    @Column(name = "event_id", nullable = false)
    private String eventId;
    
    @NotNull
    @Column(name = "scanner_user_id", nullable = false)
    private String scannerUserId;
    
    @Column(name = "staff_assignment_id")
    private String staffAssignmentId;
    
    @Column(name = "gate_id")
    private String gateId;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScanResult result;
    
    @Size(max = 100)
    @Column(name = "reason_code", length = 100)
    private String reasonCode;
    
    @NotNull
    @CreationTimestamp
    @Column(name = "scanned_at", nullable = false, updatable = false)
    private Instant scannedAt;
    
    /**
     * SHA-256 hash of device identifier (for fraud detection)
     */
    @Size(max = 64)
    @Column(name = "device_id_hash", length = 64)
    private String deviceIdHash;
    
    /**
     * Optional offline scan reference for future sync
     */
    @Size(max = 100)
    @Column(name = "offline_reference", length = 100)
    private String offlineReference;
    
    /**
     * Check if scan was successful
     */
    public boolean isSuccessful() {
        return result == ScanResult.VALID;
    }
}
