package com.yeyamo_mobile.api.moderation_trust_service.domain.model;

/**
 * Status of copyright claim workflow
 */
public enum CopyrightClaimStatus {
    /**
     * Claim has been filed
     */
    FILED,
    
    /**
     * Content temporarily removed pending review
     */
    CONTENT_REMOVED,
    
    /**
     * Awaiting response from content creator
     */
    AWAITING_CREATOR_RESPONSE,
    
    /**
     * Under review by legal/moderation team
     */
    UNDER_REVIEW,
    
    /**
     * Claim upheld, content remains removed
     */
    UPHELD,
    
    /**
     * Claim rejected, content restored
     */
    REJECTED,
    
    /**
     * Settled between parties
     */
    SETTLED
}
