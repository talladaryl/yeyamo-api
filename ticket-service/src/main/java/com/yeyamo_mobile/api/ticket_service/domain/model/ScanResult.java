package com.yeyamo_mobile.api.ticket_service.domain.model;

/**
 * Result of a ticket scan operation
 */
public enum ScanResult {
    /**
     * Ticket is valid and entry is granted
     */
    VALID,
    
    /**
     * Ticket has already been used for entry
     */
    ALREADY_USED,
    
    /**
     * Ticket signature is invalid or token is malformed
     */
    INVALID,
    
    /**
     * Ticket has expired
     */
    EXPIRED,
    
    /**
     * Ticket has been cancelled
     */
    CANCELLED,
    
    /**
     * Ticket has been refunded
     */
    REFUNDED,
    
    /**
     * Ticket is for a different event
     */
    WRONG_EVENT,
    
    /**
     * Ticket is not valid for this gate/zone
     */
    WRONG_GATE,
    
    /**
     * Event has not started yet
     */
    NOT_YET_VALID,
    
    /**
     * Access is denied for other reasons
     */
    ACCESS_DENIED
}
