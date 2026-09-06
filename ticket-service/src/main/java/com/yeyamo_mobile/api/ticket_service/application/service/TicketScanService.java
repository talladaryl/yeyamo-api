package com.yeyamo_mobile.api.ticket_service.application.service;

import com.yeyamo_mobile.api.ticket_service.application.dto.TicketScanRequest;
import com.yeyamo_mobile.api.ticket_service.application.dto.TicketScanResponse;
import com.yeyamo_mobile.api.ticket_service.domain.model.*;
import com.yeyamo_mobile.api.ticket_service.domain.repository.*;
import com.yeyamo_mobile.api.ticket_service.infrastructure.security.QrTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Service for scanning and validating tickets at event entry.
 * Implements atomic scan validation to prevent double-entry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketScanService {
    
    private final TicketRepository ticketRepository;
    private final TicketScanRepository scanRepository;
    private final TicketQrCredentialRepository qrCredentialRepository;
    private final EventStaffAssignmentRepository staffRepository;
    private final QrTokenService qrTokenService;
    private final OutboxService outboxService;
    
    /**
     * Scan and validate a ticket QR code.
     * This operation must be atomic - concurrent scans of the same ticket must not both succeed.
     * 
     * @param scannerUserId User scanning the ticket
     * @param request Scan request with QR token
     * @return Scan result
     */
    @Transactional
    public TicketScanResponse scanTicket(String scannerUserId, TicketScanRequest request) {
        log.info("Scanning ticket for event: {}, scanner: {}", request.getEventId(), scannerUserId);
        
        // Validate scanner has permission
        EventStaffAssignment staffAssignment = staffRepository
                .findActiveAssignment(request.getEventId(), scannerUserId, Instant.now())
                .orElse(null);
        
        if (staffAssignment == null || !staffAssignment.canScanTickets()) {
            return recordFailedScan(null, request.getEventId(), scannerUserId, 
                    staffAssignment != null ? staffAssignment.getId() : null, 
                    request.getGateId(), request.getDeviceId(), 
                    ScanResult.ACCESS_DENIED, "Scanner not authorized");
        }
        
        // Validate QR token signature
        QrTokenService.TokenValidationResult tokenValidation = qrTokenService.validateToken(request.getQrToken());
        
        if (!tokenValidation.isValid()) {
            return recordFailedScan(null, request.getEventId(), scannerUserId, 
                    staffAssignment.getId(), request.getGateId(), request.getDeviceId(),
                    ScanResult.INVALID, tokenValidation.getErrorMessage());
        }
        
        String ticketId = tokenValidation.getTicketId();
        String tokenEventId = tokenValidation.getEventId();
        
        // Verify event matches
        if (!request.getEventId().equals(tokenEventId)) {
            return recordFailedScan(ticketId, request.getEventId(), scannerUserId, 
                    staffAssignment.getId(), request.getGateId(), request.getDeviceId(),
                    ScanResult.WRONG_EVENT, "Ticket is for a different event");
        }
        
        // Check if token is revoked
        TicketQrCredential credential = qrCredentialRepository.findByTokenId(tokenValidation.getTokenId())
                .orElse(null);
        
        if (credential == null) {
            return recordFailedScan(ticketId, request.getEventId(), scannerUserId, 
                    staffAssignment.getId(), request.getGateId(), request.getDeviceId(),
                    ScanResult.INVALID, "Token not found in system");
        }
        
        if (credential.isRevoked()) {
            return recordFailedScan(ticketId, request.getEventId(), scannerUserId, 
                    staffAssignment.getId(), request.getGateId(), request.getDeviceId(),
                    ScanResult.INVALID, "Token has been revoked");
        }
        
        // Load ticket with pessimistic lock to prevent concurrent scans
        Ticket ticket = ticketRepository.findByIdWithLock(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        
        // Check if ticket already used
        if (ticket.getStatus() == TicketStatus.USED) {
            TicketScan previousScan = scanRepository.findSuccessfulScanByTicketId(ticketId)
                    .orElse(null);
            
            return recordFailedScan(ticketId, request.getEventId(), scannerUserId, 
                    staffAssignment.getId(), request.getGateId(), request.getDeviceId(),
                    ScanResult.ALREADY_USED, "Ticket already used",
                    previousScan != null ? previousScan.getScannedAt() : null);
        }

        // Validate ticket status
        ScanResult validationResult = validateTicketStatus(ticket);
        if (validationResult != ScanResult.VALID) {
            return recordFailedScan(ticketId, request.getEventId(), scannerUserId,
                    staffAssignment.getId(), request.getGateId(), request.getDeviceId(),
                    validationResult, getReasonForResult(validationResult));
        }
        
        // ATOMIC: Mark ticket as used
        ticket.markAsUsed();
        ticketRepository.save(ticket);
        
        // Record successful scan
        TicketScan scan = recordSuccessfulScan(ticketId, request.getEventId(), 
                scannerUserId, staffAssignment.getId(), request.getGateId(), 
                request.getDeviceId());
        
        // Publish ticket used event
        outboxService.publishTicketUsed(ticket, scan);
        
        log.info("Ticket {} successfully scanned at event {}", ticketId, request.getEventId());
        
        return TicketScanResponse.builder()
                .result(ScanResult.VALID)
                .ticketReference(ticket.getSerialNumber())
                .eventSummary("Event " + request.getEventId())
                .ownerDisplayName(maskUserName(ticket.getOwnerUserId()))
                .usedAt(ticket.getUsedAt())
                .message("Entry granted - Welcome!")
                .build();
    }
    
    /**
     * Validate ticket status for entry
     */
    private ScanResult validateTicketStatus(Ticket ticket) {
        return switch (ticket.getStatus()) {
            case VALID -> ScanResult.VALID;
            case USED -> ScanResult.ALREADY_USED;
            case CANCELLED -> ScanResult.CANCELLED;
            case REFUNDED -> ScanResult.REFUNDED;
            case EXPIRED -> ScanResult.EXPIRED;
            case REVOKED -> ScanResult.INVALID;
            case PENDING_PAYMENT -> ScanResult.ACCESS_DENIED;
        };
    }
    
    private String getReasonForResult(ScanResult result) {
        return switch (result) {
            case ALREADY_USED -> "Ticket has already been used for entry";
            case CANCELLED -> "Ticket has been cancelled";
            case REFUNDED -> "Ticket has been refunded";
            case EXPIRED -> "Ticket has expired";
            case INVALID -> "Ticket is not valid";
            case ACCESS_DENIED -> "Entry not permitted";
            default -> "Unknown reason";
        };
    }
    
    /**
     * Record successful scan
     */
    private TicketScan recordSuccessfulScan(String ticketId, String eventId, 
                                           String scannerUserId, String staffAssignmentId,
                                           String gateId, String deviceId) {
        TicketScan scan = TicketScan.builder()
                .ticketId(ticketId)
                .eventId(eventId)
                .scannerUserId(scannerUserId)
                .staffAssignmentId(staffAssignmentId)
                .gateId(gateId)
                .result(ScanResult.VALID)
                .deviceIdHash(hashDeviceId(deviceId))
                .build();
        
        return scanRepository.save(scan);
    }
    
    /**
     * Record failed scan attempt
     */
    private TicketScanResponse recordFailedScan(String ticketId, String eventId, 
                                               String scannerUserId, String staffAssignmentId,
                                               String gateId, String deviceId,
                                               ScanResult result, String reasonCode) {
        return recordFailedScan(ticketId, eventId, scannerUserId, staffAssignmentId, 
                               gateId, deviceId, result, reasonCode, null);
    }
    
    private TicketScanResponse recordFailedScan(String ticketId, String eventId, 
                                               String scannerUserId, String staffAssignmentId,
                                               String gateId, String deviceId,
                                               ScanResult result, String reasonCode,
                                               Instant previousScanTime) {
        TicketScan scan = TicketScan.builder()
                .ticketId(ticketId)
                .eventId(eventId)
                .scannerUserId(scannerUserId)
                .staffAssignmentId(staffAssignmentId)
                .gateId(gateId)
                .result(result)
                .reasonCode(reasonCode)
                .deviceIdHash(hashDeviceId(deviceId))
                .build();
        
        scanRepository.save(scan);
        
        return TicketScanResponse.builder()
                .result(result)
                .reasonCode(reasonCode)
                .usedAt(previousScanTime)
                .message("Entry denied - " + reasonCode)
                .build();
    }
    
    /**
     * Hash device ID for privacy
     */
    private String hashDeviceId(String deviceId) {
        if (deviceId == null) {
            return null;
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(deviceId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 not available", e);
            return null;
        }
    }
    
    /**
     * Mask user name for privacy
     */
    private String maskUserName(String userId) {
        // In production, fetch user name and mask it (e.g., "John D***")
        return "User " + userId.substring(0, Math.min(4, userId.length())) + "***";
    }
}
