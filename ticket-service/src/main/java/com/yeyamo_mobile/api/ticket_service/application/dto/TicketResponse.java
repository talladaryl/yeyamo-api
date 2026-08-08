package com.yeyamo_mobile.api.ticket_service.application.dto;

import com.yeyamo_mobile.api.ticket_service.domain.model.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response containing ticket details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    
    private String id;
    
    private String serialNumber;
    
    private String eventId;
    
    private String eventName;
    
    private String ticketTypeId;
    
    private String ticketTypeName;
    
    private String accessZone;
    
    private BigDecimal price;
    
    private String currency;
    
    private TicketStatus status;
    
    private Instant issuedAt;
    
    private Instant usedAt;
    
    private String qrToken;
    
    private Instant eventStartsAt;
}
