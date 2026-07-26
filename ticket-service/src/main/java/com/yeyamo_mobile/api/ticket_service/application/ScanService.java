package com.yeyamo_mobile.api.ticket_service.application;

import com.yeyamo_mobile.api.ticket_service.domain.model.ScanResult;
import com.yeyamo_mobile.api.ticket_service.domain.model.TicketStatus;
import com.yeyamo_mobile.api.ticket_service.infrastructure.crypto.QrTokenService;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.*;
import com.yeyamo_mobile.api.ticket_service.infrastructure.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ScanService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScanService.class);
    
    private final QrTokenService qrTokenService;
    private final SpringTicketRepository ticketRepository;
    private final SpringTicketScanRepository scanRepository;
    private final SpringEventStaffAssignmentRepository staffRepository;
    private final SpringTicketTypeRepository ticketTypeRepository;
    private final OutboxService outbox;
    
    public ScanService(
            QrTokenService qrTokenService,
            SpringTicketRepository ticketRepository,
            SpringTicketScanRepository scanRepository,
            SpringEventStaffAssignmentRepository staffRepository,
            SpringTicketTypeRepository ticketTypeRepository,
            OutboxService outbox) {
        this.qrTokenService = qrTokenService;
        this.ticketRepository = ticketRepository;
        this.scanRepository = scanRepository;
        this.staffRepository = staffRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.outbox = outbox;
    }
    
    /**
     * Scan ticket with atomic validation
     * Ensures no double-scan is possible
     */
    @Transactional
    public ScanResponse scanTicket(ScanRequest request) {
        logger.info("Scanning ticket for event: {} by scanner: {}", 
            request.eventId(), request.scannerUserId());

        if (request.offlineReference() != null) {
            Optional<TicketScanEntity> replay = scanRepository
                .findByScannerUserIdAndOfflineReference(
                    request.scannerUserId(), request.offlineReference()
                );
            if (replay.isPresent()) {
                TicketScanEntity saved = replay.get();
                TicketEntity ticket = saved.getTicketId() == null ? null
                    : ticketRepository.findById(saved.getTicketId()).orElse(null);
                return saved.getResult() == ScanResult.VALID && ticket != null
                    ? buildSuccessResponse(ticket, saved)
                    : buildFailureResponse(saved.getResult(), saved.getReasonCode(), ticket, null);
            }
        }
        
        // 1. Verify staff authorization
        if (!isStaffAuthorized(request.scannerUserId(), request.eventId())) {
            return recordFailedScan(request, ScanResult.ACCESS_DENIED, null, 
                "Scanner not authorized for this event");
        }
        
        // 2. Validate QR token
        QrTokenService.QrValidationResult validation = qrTokenService
            .validateQrToken(request.qrToken(), request.eventId());
        
        if (!validation.isValid()) {
            return recordFailedScan(request, 
                mapQrErrorToScanResult(validation.getErrorCode()), 
                null, 
                validation.getErrorMessage());
        }
        
        UUID ticketId = validation.getTicketId();
        
        // 3. Load ticket with PESSIMISTIC WRITE lock (prevents concurrent scans)
        Optional<TicketEntity> ticketOpt = ticketRepository.findByIdForUpdate(ticketId);
        
        if (ticketOpt.isEmpty()) {
            return recordFailedScan(request, ScanResult.INVALID, null, 
                "Ticket not found");
        }
        
        TicketEntity ticket = ticketOpt.get();
        
        // 4. Verify event match
        if (!ticket.getEventId().equals(request.eventId())) {
            return recordFailedScan(request, ScanResult.WRONG_EVENT, ticket, 
                "Ticket is for different event");
        }
        
        // 5. Validate ticket status
        if (ticket.getStatus() == TicketStatus.USED) {
            // Already used - get first scan info
            Optional<TicketScanEntity> firstScan = scanRepository
                .findFirstSuccessfulScan(ticketId);
            
            return recordFailedScan(request, ScanResult.ALREADY_USED, ticket, 
                "Ticket already scanned",
                firstScan.orElse(null));
        }
        
        if (ticket.getStatus() != TicketStatus.VALID) {
            ScanResult result = mapTicketStatusToScanResult(ticket.getStatus());
            return recordFailedScan(request, result, ticket, 
                "Ticket status: " + ticket.getStatus());
        }
        
        // 6. Validate gate (if specified)
        if (request.gateId() != null && !isGateAllowed(ticket, request.gateId())) {
            return recordFailedScan(request, ScanResult.WRONG_GATE, ticket, 
                "Ticket not valid for this gate");
        }
        
        // 7. ATOMIC UPDATE: Mark ticket as USED
        ticket.markAsUsed();
        ticketRepository.save(ticket);
        
        // 8. Record successful scan
        TicketScanEntity scan = createScanRecord(request, ticket, ScanResult.VALID, null);
        scanRepository.save(scan);
        outbox.publishTicketScanned(ticket.getId().toString(), ticket.getEventId(),
            ticket.getOwnerUserId(), request.scannerUserId());
        
        logger.info("Ticket scanned successfully: {} for event: {}", ticketId, request.eventId());
        
        // 9. Build response
        return buildSuccessResponse(ticket, scan);
    }
    
    /**
     * Get scan statistics for event
     */
    @Transactional(readOnly = true)
    public ScanStatistics getScanStatistics(String eventId, String requestingUserId) {
        if (!isStaffAuthorized(requestingUserId, eventId)) {
            throw new SecurityException("Staff member is not assigned to this event");
        }
        long validScans = scanRepository.countByEventIdAndResult(eventId, ScanResult.VALID);
        long alreadyUsed = scanRepository.countByEventIdAndResult(eventId, ScanResult.ALREADY_USED);
        long invalid = scanRepository.countByEventIdAndResult(eventId, ScanResult.INVALID);
        long accessDenied = scanRepository.countByEventIdAndResult(eventId, ScanResult.ACCESS_DENIED);
        
        long totalScans = validScans + alreadyUsed + invalid + accessDenied;
        
        return new ScanStatistics(
            eventId,
            validScans,
            alreadyUsed,
            invalid,
            accessDenied,
            totalScans,
            totalScans > 0 ? (double) validScans / totalScans * 100 : 0.0
        );
    }
    
    private boolean isStaffAuthorized(String userId, String eventId) {
        return staffRepository.existsActiveAssignment(userId, eventId, Instant.now());
    }
    
    private boolean isGateAllowed(TicketEntity ticket, String requestedGate) {
        // Load ticket type to check access zone
        TicketTypeEntity ticketType = ticketTypeRepository
            .findById(ticket.getTicketTypeId())
            .orElse(null);
        
        if (ticketType == null || ticketType.getAccessZone() == null) {
            return true; // No restrictions
        }
        
        // Simple implementation: check if gate matches access zone
        return ticketType.getAccessZone().equalsIgnoreCase(requestedGate);
    }
    
    private ScanResponse recordFailedScan(
            ScanRequest request, 
            ScanResult result, 
            TicketEntity ticket,
            String reason) {
        return recordFailedScan(request, result, ticket, reason, null);
    }
    
    private ScanResponse recordFailedScan(
            ScanRequest request, 
            ScanResult result, 
            TicketEntity ticket,
            String reason,
            TicketScanEntity firstScan) {
        
        TicketScanEntity scan = createScanRecord(request, ticket, result, reason);
        scanRepository.save(scan);
        outbox.publishScanRejected(request.eventId(), request.scannerUserId(),
            result.name());
        
        logger.warn("Ticket scan failed: {} for event: {} - Reason: {}", 
            result, request.eventId(), reason);
        
        return buildFailureResponse(result, reason, ticket, firstScan);
    }
    
    private TicketScanEntity createScanRecord(
            ScanRequest request, 
            TicketEntity ticket, 
            ScanResult result,
            String reason) {
        
        TicketScanEntity scan = new TicketScanEntity();
        scan.setTicketId(ticket != null ? ticket.getId() : null);
        scan.setEventId(request.eventId());
        scan.setScannerUserId(request.scannerUserId());
        scan.setGateId(request.gateId());
        scan.setResult(result);
        scan.setReasonCode(reason);
        scan.setScannedAt(request.scannedAt() != null ? request.scannedAt() : Instant.now());
        scan.setDeviceIdHash(hashDeviceId(request.deviceId()));
        scan.setOfflineReference(request.offlineReference());
        
        return scan;
    }
    
    private ScanResponse buildSuccessResponse(TicketEntity ticket, TicketScanEntity scan) {
        TicketTypeEntity ticketType = ticketTypeRepository
            .findById(ticket.getTicketTypeId())
            .orElse(null);
        
        return new ScanResponse(
            ScanResult.VALID,
            null,
            maskTicketReference(ticket.getSerialNumber()),
            ticket.getEventId(),
            ticketType != null ? ticketType.getName() : "Unknown",
            ticketType != null ? ticketType.getAccessZone() : null,
            maskUserId(ticket.getOwnerUserId()),
            ticket.getUsedAt(),
            null,
            scan.getId()
        );
    }
    
    private ScanResponse buildFailureResponse(
            ScanResult result, 
            String reason, 
            TicketEntity ticket,
            TicketScanEntity firstScan) {
        
        String firstScannerName = null;
        if (firstScan != null) {
            firstScannerName = maskUserId(firstScan.getScannerUserId());
        }
        
        return new ScanResponse(
            result,
            reason,
            ticket != null ? maskTicketReference(ticket.getSerialNumber()) : null,
            ticket != null ? ticket.getEventId() : null,
            null,
            null,
            ticket != null ? maskUserId(ticket.getOwnerUserId()) : null,
            ticket != null ? ticket.getUsedAt() : null,
            firstScannerName,
            null
        );
    }
    
    private ScanResult mapQrErrorToScanResult(String errorCode) {
        return switch (errorCode) {
            case "EXPIRED" -> ScanResult.EXPIRED;
            case "REVOKED" -> ScanResult.CANCELLED;
            case "WRONG_EVENT" -> ScanResult.WRONG_EVENT;
            default -> ScanResult.INVALID;
        };
    }
    
    private ScanResult mapTicketStatusToScanResult(TicketStatus status) {
        return switch (status) {
            case CANCELLED -> ScanResult.CANCELLED;
            case REFUNDED -> ScanResult.REFUNDED;
            case EXPIRED -> ScanResult.EXPIRED;
            case REVOKED -> ScanResult.CANCELLED;
            default -> ScanResult.INVALID;
        };
    }
    
    private String maskTicketReference(String reference) {
        if (reference == null || reference.length() < 8) {
            return "***";
        }
        return reference.substring(0, 4) + "***" + reference.substring(reference.length() - 4);
    }
    
    private String maskUserId(String userId) {
        if (userId == null || userId.length() < 4) {
            return "***";
        }
        return userId.substring(0, 2) + "***";
    }
    
    private String hashDeviceId(String deviceId) {
        if (deviceId == null) {
            return null;
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(deviceId.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).substring(0, 16); // First 16 chars
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    // DTOs
    public record ScanRequest(
        String qrToken,
        String eventId,
        String scannerUserId,
        String gateId,
        String deviceId,
        String offlineReference,
        Instant scannedAt
    ) {}
    
    public record ScanResponse(
        ScanResult result,
        String reasonCode,
        String ticketReferenceMasked,
        String eventId,
        String ticketTypeName,
        String accessZone,
        String ownerDisplayNameMasked,
        Instant usedAt,
        String firstScannerNameMasked,
        UUID scanId
    ) {
        public boolean isSuccess() {
            return result == ScanResult.VALID;
        }
    }
    
    public record ScanStatistics(
        String eventId,
        long validScans,
        long alreadyUsedAttempts,
        long invalidAttempts,
        long accessDeniedAttempts,
        long totalScans,
        double successRate
    ) {}
}
