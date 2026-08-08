package com.yeyamo_mobile.api.ticket_service.domain.model;

/**
 * Status of a ticket through its lifecycle
 */
public enum TicketStatus {
    /**
     * Ticket payment is pending
     */
    PENDING_PAYMENT,
    
    /**
     * Ticket is valid and can be used for entry
     */
    VALID,
    
    /**
     * Ticket has been used for event entry
     */
    USED,
    
    /**
     * Ticket has been cancelled before use
     */
    CANCELLED,
    
    /**
     * Ticket has been refunded
     */
    REFUNDED,
    
    /**
     * Ticket has expired (event passed or validity period ended)
     */
    EXPIRED,
    
    /**
     * Ticket QR credential has been revoked for security reasons
     */
    REVOKED
}
