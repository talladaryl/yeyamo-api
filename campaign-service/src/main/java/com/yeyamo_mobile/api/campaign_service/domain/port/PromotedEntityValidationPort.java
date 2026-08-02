package com.yeyamo_mobile.api.campaign_service.domain.port;

import com.yeyamo_mobile.api.campaign_service.domain.model.PromotedEntityType;

/**
 * Port for validating promoted entities (PLACE, EVENT, POST, etc.)
 * Implementation will call respective services or use local projections
 */
public interface PromotedEntityValidationPort {
    
    /**
     * Check if the promoted entity exists
     * @param entityType the type of entity
     * @param entityId the entity ID
     * @return true if entity exists
     */
    boolean exists(PromotedEntityType entityType, String entityId);
}
