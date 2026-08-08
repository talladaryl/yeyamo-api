package com.yeyamo_mobile.api.ticket_service.domain.model;

/**
 * Status of a ticket order through its lifecycle
 */
public enum TicketOrderStatus {
    /**
     * Order has been created and inventory held
     */
    CREATED,
    
    /**
     * Waiting for payment confirmation
     */
    AWAITING_PAYMENT,
    
    /**
     * Payment has been confirmed
     */
    PAID,
    
    /**
     * Tickets have been issued with QR codes
     */
    ISSUED,
    
    /**
     * Order has been cancelled before payment
     */
    CANCELLED,
    
    /**
     * Order has expired (payment not received in time)
     */
    EXPIRED,
    
    /**
     * Full refund has been processed
     */
    REFUNDED,
    
    /**
     * Partial refund has been processed
     */
    PARTIALLY_REFUNDED
}
