package com.yeyamo_mobile.api.ticket_service.application.dto;

import com.yeyamo_mobile.api.ticket_service.domain.model.TicketOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response containing ticket order details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketOrderResponse {
    
    private String id;
    
    private String reference;
    
    private String userId;
    
    private String eventId;
    
    private TicketOrderStatus status;
    
    private BigDecimal subtotal;
    
    private BigDecimal discountAmount;
    
    private BigDecimal serviceFee;
    
    private BigDecimal totalAmount;
    
    private String currency;
    
    private List<TicketItemSummary> items;
    
    private Instant createdAt;
    
    private Instant expiresAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TicketItemSummary {
        private String ticketTypeId;
        private String ticketTypeName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}
