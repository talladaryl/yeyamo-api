package com.yeyamo_mobile.api.ticket_service.interfaces.rest;

import com.yeyamo_mobile.api.ticket_service.application.ScanService;
import com.yeyamo_mobile.api.ticket_service.domain.model.ScanResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Ticket Scanning", description = "Ticket validation and scanning operations")
@SecurityRequirement(name = "bearerAuth")
public class ScanController {
    
    private final ScanService scanService;
    
    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }
    
    @PostMapping("/scan")
    @Operation(summary = "Scan ticket (validate and mark as used)")
    public ResponseEntity<ScanResponseDto> scanTicket(
            @Valid @RequestBody ScanRequestDto request,
            Authentication auth) {
        
        String scannerUserId = auth.getName();
        
        ScanService.ScanResponse response = scanService.scanTicket(
            new ScanService.ScanRequest(
                request.qrToken(),
                request.eventId(),
                scannerUserId,
                request.gateId(),
                request.deviceId(),
                request.offlineReference(),
                request.scannedAt()
            )
        );
        
        return ResponseEntity.ok(new ScanResponseDto(
            response.result(),
            response.reasonCode(),
            response.ticketReferenceMasked(),
            response.eventId(),
            response.ticketTypeName(),
            response.accessZone(),
            response.ownerDisplayNameMasked(),
            response.usedAt(),
            response.firstScannerNameMasked(),
            response.scanId()
        ));
    }
    
    @GetMapping("/scans/stats/{eventId}")
    @Operation(summary = "Get scan statistics for event")
    public ResponseEntity<ScanStatisticsDto> getScanStatistics(
            @PathVariable String eventId,
            Authentication auth) {
        
        ScanService.ScanStatistics stats = scanService.getScanStatistics(eventId, auth.getName());
        
        return ResponseEntity.ok(new ScanStatisticsDto(
            stats.eventId(),
            stats.validScans(),
            stats.alreadyUsedAttempts(),
            stats.invalidAttempts(),
            stats.accessDeniedAttempts(),
            stats.totalScans(),
            stats.successRate()
        ));
    }
    
    // DTOs
    public record ScanRequestDto(
        @NotBlank String qrToken,
        @NotBlank String eventId,
        String gateId,
        String deviceId,
        String offlineReference,
        Instant scannedAt
    ) {}
    
    public record ScanResponseDto(
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
    
    public record ScanStatisticsDto(
        String eventId,
        long validScans,
        long alreadyUsedAttempts,
        long invalidAttempts,
        long accessDeniedAttempts,
        long totalScans,
        double successRate
    ) {}
}
