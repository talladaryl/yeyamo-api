package com.yeyamo_mobile.api.ticket_service.infrastructure.persistence;

import com.yeyamo_mobile.api.ticket_service.domain.model.ScanResult;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_scans")
public class TicketScanEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;
    
    @Column(name = "event_id", nullable = false)
    private String eventId;
    
    @Column(name = "scanner_user_id", nullable = false)
    private String scannerUserId;
    
    @Column(name = "staff_assignment_id")
    private UUID staffAssignmentId;
    
    @Column(name = "gate_id", length = 100)
    private String gateId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ScanResult result;
    
    @Column(name = "reason_code", length = 100)
    private String reasonCode;
    
    @Column(name = "scanned_at", nullable = false)
    private Instant scannedAt;
    
    @Column(name = "device_id_hash")
    private String deviceIdHash;
    
    @Column(name = "offline_reference")
    private String offlineReference;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getTicketId() { return ticketId; }
    public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }
    
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getScannerUserId() { return scannerUserId; }
    public void setScannerUserId(String scannerUserId) { 
        this.scannerUserId = scannerUserId; 
    }
    
    public UUID getStaffAssignmentId() { return staffAssignmentId; }
    public void setStaffAssignmentId(UUID staffAssignmentId) { 
        this.staffAssignmentId = staffAssignmentId; 
    }
    
    public String getGateId() { return gateId; }
    public void setGateId(String gateId) { this.gateId = gateId; }
    
    public ScanResult getResult() { return result; }
    public void setResult(ScanResult result) { this.result = result; }
    
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    
    public Instant getScannedAt() { return scannedAt; }
    public void setScannedAt(Instant scannedAt) { this.scannedAt = scannedAt; }
    
    public String getDeviceIdHash() { return deviceIdHash; }
    public void setDeviceIdHash(String deviceIdHash) { this.deviceIdHash = deviceIdHash; }
    
    public String getOfflineReference() { return offlineReference; }
    public void setOfflineReference(String offlineReference) { 
        this.offlineReference = offlineReference; 
    }
    
    public Instant getCreatedAt() { return createdAt; }
}
