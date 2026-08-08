package com.yeyamo_mobile.api.ticket_service.presentation.controller;

import com.yeyamo_mobile.api.ticket_service.application.dto.*;
import com.yeyamo_mobile.api.ticket_service.application.service.TicketScanService;
import com.yeyamo_mobile.api.ticket_service.domain.model.*;
import com.yeyamo_mobile.api.ticket_service.domain.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Partner APIs for ticket management, staff assignment, and scanning
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/partner/tickets")
@RequiredArgsConstructor
@Tag(name = "Partner Ticket Management", description = "APIs for partners to manage ticketing")
@SecurityRequirement(name = "bearer-jwt")
public class PartnerTicketController {
    
    private final TicketSaleConfigurationRepository saleConfigRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final EventStaffAssignmentRepository staffRepository;
    private final TicketScanService scanService;
    private final TicketScanRepository scanRepository;
    
    /**
     * Create ticket sales configuration for an event
     */
    @PostMapping("/configurations")
    @Operation(summary = "Create ticket sales configuration")
    public ResponseEntity<TicketSaleConfiguration> createSaleConfiguration(
            Authentication authentication,
            @Valid @RequestBody CreateSaleConfigurationRequest request) {
        
        String partnerId = extractPartnerId(authentication);
        log.info("Creating sale configuration for event: {}, partner: {}", 
                 request.getEventId(), partnerId);
        
        // Create configuration
        TicketSaleConfiguration config = TicketSaleConfiguration.builder()
                .eventId(request.getEventId())
                .partnerId(partnerId)
                .salesStartAt(request.getSalesStartAt())
                .salesEndAt(request.getSalesEndAt())
                .maxTicketsPerBuyer(request.getMaxTicketsPerBuyer())
                .currency(request.getCurrency())
                .status(SaleStatus.DRAFT)
                .build();
        
        config = saleConfigRepository.save(config);
        
        // Create ticket types
        for (var typeReq : request.getTicketTypes()) {
            TicketType ticketType = TicketType.builder()
                    .saleConfigurationId(config.getId())
                    .code(typeReq.getCode())
                    .name(typeReq.getName())
                    .description(typeReq.getDescription())
                    .price(typeReq.getPrice())
                    .quantityTotal(typeReq.getQuantityTotal())
                    .quantityReserved(0)
                    .quantitySold(0)
                    .salesStartAt(typeReq.getSalesStartAt())
                    .salesEndAt(typeReq.getSalesEndAt())
                    .accessZone(typeReq.getAccessZone())
                    .gateInstructions(typeReq.getGateInstructions())
                    .status(SaleStatus.DRAFT)
                    .build();
            
            ticketTypeRepository.save(ticketType);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(config);
    }
    
    /**
     * Activate ticket sales
     */
    @PostMapping("/configurations/{configId}/activate")
    @Operation(summary = "Activate ticket sales")
    public ResponseEntity<Void> activateSales(
            Authentication authentication,
            @PathVariable String configId) {
        
        String partnerId = extractPartnerId(authentication);
        
        TicketSaleConfiguration config = saleConfigRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found"));
        
        if (!config.getPartnerId().equals(partnerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        config.setStatus(SaleStatus.ACTIVE);
        saleConfigRepository.save(config);
        
        // Activate all ticket types
        List<TicketType> types = ticketTypeRepository.findBySaleConfigurationId(configId);
        types.forEach(type -> type.setStatus(SaleStatus.ACTIVE));
        ticketTypeRepository.saveAll(types);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Create staff assignment
     */
    @PostMapping("/staff")
    @Operation(summary = "Assign staff to event")
    public ResponseEntity<EventStaffAssignment> createStaffAssignment(
            Authentication authentication,
            @Valid @RequestBody CreateStaffAssignmentRequest request) {
        
        String partnerId = extractPartnerId(authentication);
        
        EventStaffAssignment assignment = EventStaffAssignment.builder()
                .eventId(request.getEventId())
                .partnerId(partnerId)
                .userId(request.getUserId())
                .role(request.getRole())
                .status(StaffStatus.ACTIVE)
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .build();
        
        assignment = staffRepository.save(assignment);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }
    
    /**
     * Get staff for an event
     */
    @GetMapping("/events/{eventId}/staff")
    @Operation(summary = "Get event staff")
    public ResponseEntity<List<EventStaffAssignment>> getEventStaff(
            Authentication authentication,
            @PathVariable String eventId) {
        
        List<EventStaffAssignment> staff = staffRepository.findByEventId(eventId);
        return ResponseEntity.ok(staff);
    }
    
    /**
     * Revoke staff assignment
     */
    @DeleteMapping("/staff/{assignmentId}")
    @Operation(summary = "Revoke staff assignment")
    public ResponseEntity<Void> revokeStaffAssignment(
            Authentication authentication,
            @PathVariable String assignmentId) {
        
        EventStaffAssignment assignment = staffRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));
        
        assignment.setStatus(StaffStatus.REVOKED);
        staffRepository.save(assignment);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Scan ticket (staff endpoint)
     */
    @PostMapping("/scan")
    @Operation(summary = "Scan ticket QR code")
    public ResponseEntity<TicketScanResponse> scanTicket(
            Authentication authentication,
            @Valid @RequestBody TicketScanRequest request) {
        
        String scannerUserId = extractUserId(authentication);
        
        TicketScanResponse response = scanService.scanTicket(scannerUserId, request);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get scan statistics for an event
     */
    @GetMapping("/events/{eventId}/scan-stats")
    @Operation(summary = "Get scan statistics")
    public ResponseEntity<Map<String, Object>> getScanStats(
            Authentication authentication,
            @PathVariable String eventId) {
        
        List<Object[]> results = scanRepository.countScanResultsByEvent(eventId);
        long totalScans = scanRepository.findByEventId(eventId).size();
        long successfulScans = scanRepository.countSuccessfulScansByEvent(eventId);
        
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalScans", totalScans);
        stats.put("successfulScans", successfulScans);
        stats.put("resultBreakdown", results.stream()
                .collect(Collectors.toMap(
                        r -> r[0].toString(),
                        r -> r[1]
                )));
        
        return ResponseEntity.ok(stats);
    }
    
    private String extractPartnerId(Authentication authentication) {
        // Extract from JWT claims
        return authentication.getName();
    }
    
    private String extractUserId(Authentication authentication) {
        return authentication.getName();
    }
}
