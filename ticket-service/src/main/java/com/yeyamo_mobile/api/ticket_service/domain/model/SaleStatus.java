package com.yeyamo_mobile.api.ticket_service.domain.model;

/**
 * Status of ticket sales configuration
 */
public enum SaleStatus {
    /**
     * Sales configuration is in draft mode
     */
    DRAFT,
    
    /**
     * Sales are active
     */
    ACTIVE,
    
    /**
     * Sales are temporarily paused
     */
    PAUSED,
    
    /**
     * Sales have ended
     */
    ENDED,
    
    /**
     * Sales configuration is cancelled
     */
    CANCELLED
}
