package com.yeyamo_mobile.api.ticket_service.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Request to create ticket sales configuration for an event
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSaleConfigurationRequest {
    
    @NotBlank(message = "Event ID is required")
    private String eventId;
    
    @NotNull(message = "Sales start time is required")
    private Instant salesStartAt;
    
    @NotNull(message = "Sales end time is required")
    private Instant salesEndAt;
    
    @Min(1)
    @Max(20)
    private Integer maxTicketsPerBuyer = 10;
    
    @Size(min = 3, max = 3)
    private String currency = "XOF";
    
    @NotEmpty(message = "At least one ticket type is required")
    @Valid
    private List<TicketTypeRequest> ticketTypes;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TicketTypeRequest {
        
        @NotBlank
        @Pattern(regexp = "^[A-Z0-9_]+$")
        private String code;
        
        @NotBlank
        @Size(min = 3, max = 100)
        private String name;
        
        @Size(max = 500)
        private String description;
        
        @NotNull
        @DecimalMin("0.00")
        private BigDecimal price;
        
        @NotNull
        @Min(1)
        private Integer quantityTotal;
        
        private Instant salesStartAt;
        
        private Instant salesEndAt;
        
        @Size(max = 100)
        private String accessZone;
        
        @Size(max = 500)
        private String gateInstructions;
    }
}
