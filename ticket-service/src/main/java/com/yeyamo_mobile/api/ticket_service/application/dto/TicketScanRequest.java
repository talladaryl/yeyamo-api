package com.yeyamo_mobile.api.ticket_service.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request to scan a ticket QR code
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketScanRequest {
    
    @NotBlank(message = "QR token is required")
    private String qrToken;
    
    @NotBlank(message = "Event ID is required")
    @Size(max = 100)
    private String eventId;
    
    @Size(max = 100)
    private String gateId;
    
    @Size(max = 100)
    private String deviceId;
    
    @Size(max = 100)
    private String clientScanReference;
    
    private Instant scannedAtClient;
}
