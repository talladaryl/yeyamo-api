package com.yeyamo_mobile.api.ticket_service.domain.model;

/**
 * Status of a temporary ticket hold
 */
public enum HoldStatus {
    /**
     * Hold is active and inventory is reserved
     */
    ACTIVE,
    
    /**
     * Hold has been converted to a confirmed order
     */
    CONFIRMED,
    
    /**
     * Hold has been released (cancelled by user or expired)
     */
    RELEASED,
    
    /**
     * Hold has expired and inventory was auto-released
     */
    EXPIRED
}
