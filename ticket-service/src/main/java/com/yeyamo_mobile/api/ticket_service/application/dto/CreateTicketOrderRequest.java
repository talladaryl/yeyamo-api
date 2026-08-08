package com.yeyamo_mobile.api.ticket_service.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to create a new ticket order
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTicketOrderRequest {
    
    @NotBlank(message = "Event ID is required")
    private String eventId;
    
    @NotEmpty(message = "At least one ticket item is required")
    @Size(max = 20, message = "Cannot order more than 20 ticket types at once")
    @Valid
    private List<TicketOrderItem> items;
    
    private String promotionCode;
    
    @NotBlank(message = "Idempotency key is required")
    @Size(min = 20, max = 100)
    private String idempotencyKey;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TicketOrderItem {
        
        @NotBlank(message = "Ticket type ID is required")
        private String ticketTypeId;
        
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 20, message = "Cannot order more than 20 tickets of same type")
        private Integer quantity;
    }
}
