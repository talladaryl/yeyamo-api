package com.yeyamo_mobile.api.campaign_service.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RejectCampaignRequest {
    
    @NotBlank(message = "rejectionReason is required")
    @Size(min = 10, max = 1000, message = "rejectionReason must be between 10 and 1000 characters")
    private String rejectionReason;

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
