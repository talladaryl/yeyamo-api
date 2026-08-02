package com.yeyamo_mobile.api.campaign_service.domain.port;

/**
 * Port for validating partner status
 * Implementation will call partner-service or use local projection
 */
public interface PartnerValidationPort {
    
    /**
     * Check if partner is valid (exists and is validated)
     * @param partnerId the partner ID
     * @return true if partner is validated and can create campaigns
     */
    boolean isValidPartner(String partnerId);
}
