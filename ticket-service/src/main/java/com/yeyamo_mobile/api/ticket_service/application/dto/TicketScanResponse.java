package com.yeyamo_mobile.api.ticket_service.application.dto;

import com.yeyamo_mobile.api.ticket_service.domain.model.ScanResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response after scanning a ticket
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketScanResponse {
    
    private ScanResult result;
    
    private String reasonCode;
    
    private String ticketReference;
    
    private String eventSummary;
    
    private String ticketType;
    
    private String accessZone;
    
    private String ownerDisplayName;
    
    private Instant usedAt;
    
    private String firstScannerDisplayName;
    
    private String message;
}
