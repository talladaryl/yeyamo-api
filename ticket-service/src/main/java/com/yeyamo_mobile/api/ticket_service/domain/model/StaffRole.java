package com.yeyamo_mobile.api.ticket_service.domain.model;

/**
 * Roles for event staff members
 */
public enum StaffRole {
    /**
     * Can manage all aspects of the event
     */
    EVENT_MANAGER,
    
    /**
     * Can scan tickets and control access at gates
     */
    ACCESS_CONTROLLER,
    
    /**
     * Can handle cash transactions (future use)
     */
    CASHIER,
    
    /**
     * Can oversee operations and handle escalations
     */
    SUPERVISOR
}
