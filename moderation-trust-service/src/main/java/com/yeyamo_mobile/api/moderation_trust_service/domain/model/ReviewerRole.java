package com.yeyamo_mobile.api.moderation_trust_service.domain.model;

/**
 * Roles for content reviewers with different scopes
 */
public enum ReviewerRole {
    /**
     * General content moderator
     */
    MODERATOR,
    
    /**
     * Cultural expert with specialized knowledge
     */
    CULTURAL_EXPERT,
    
    /**
     * Institutional reviewer (museum, university, etc.)
     */
    INSTITUTION_REVIEWER,
    
    /**
     * System administrator
     */
    ADMIN
}
