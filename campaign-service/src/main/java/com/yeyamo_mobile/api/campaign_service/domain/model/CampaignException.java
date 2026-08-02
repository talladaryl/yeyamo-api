package com.yeyamo_mobile.api.campaign_service.domain.model;

/**
 * Domain exception for campaign business rule violations
 */
public class CampaignException extends RuntimeException {
    
    public CampaignException(String message) {
        super(message);
    }
    
    public CampaignException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static CampaignException notFound(String campaignId) {
        return new CampaignException("Campaign not found: " + campaignId);
    }
    
    public static CampaignException unauthorized(String message) {
        return new CampaignException("Unauthorized: " + message);
    }
    
    public static CampaignException invalidState(String message) {
        return new CampaignException("Invalid state: " + message);
    }
    
    public static CampaignException budgetExhausted(String campaignId) {
        return new CampaignException("Campaign budget exhausted: " + campaignId);
    }
}
