package com.yeyamo_mobile.api.moderation_trust_service.domain.model;

/**
 * Status of authenticity claim verification workflow
 */
public enum AuthenticityStatus {
    /**
     * Claim has been declared by artisan/creator
     */
    DECLARED,
    
    /**
     * Evidence documents have been submitted
     */
    EVIDENCE_SUBMITTED,
    
    /**
     * Claim is under review by cultural experts
     */
    UNDER_REVIEW,
    
    /**
     * Claim has been verified as authentic
     */
    VERIFIED,
    
    /**
     * Claim has been rejected
     */
    REJECTED,
    
    /**
     * Verification is disputed
     */
    DISPUTED,
    
    /**
     * Previously verified but now revoked
     */
    REVOKED
}
