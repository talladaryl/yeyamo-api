package com.yeyamo_mobile.api.ticket_service.domain.model;

/**
 * Status of event staff assignment
 */
public enum StaffStatus {
    /**
     * Assignment is active
     */
    ACTIVE,
    
    /**
     * Assignment is suspended temporarily
     */
    SUSPENDED,
    
    /**
     * Assignment has been revoked
     */
    REVOKED
}
