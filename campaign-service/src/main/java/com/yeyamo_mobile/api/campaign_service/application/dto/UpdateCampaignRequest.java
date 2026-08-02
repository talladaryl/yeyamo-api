package com.yeyamo_mobile.api.campaign_service.application.dto;

import com.yeyamo_mobile.api.campaign_service.domain.model.BillingModel;
import com.yeyamo_mobile.api.campaign_service.domain.model.CampaignObjective;
import com.yeyamo_mobile.api.campaign_service.domain.model.CreativeConfiguration;
import com.yeyamo_mobile.api.campaign_service.domain.model.TargetConfiguration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public class UpdateCampaignRequest {
    
    @NotBlank(message = "name is required")
    @Size(min = 3, max = 255, message = "name must be between 3 and 255 characters")
    private String name;
    
    @NotNull(message = "objective is required")
    private CampaignObjective objective;
    
    @NotNull(message = "billingModel is required")
    private BillingModel billingModel;
    
    @NotNull(message = "totalBudget is required")
    @DecimalMin(value = "0.01", message = "totalBudget must be positive")
    private BigDecimal totalBudget;
    
    @NotNull(message = "dailyBudget is required")
    @DecimalMin(value = "0.01", message = "dailyBudget must be positive")
    private BigDecimal dailyBudget;
    
    @NotNull(message = "startAt is required")
    private Instant startAt;
    
    @NotNull(message = "endAt is required")
    private Instant endAt;
    
    @NotNull(message = "targetConfiguration is required")
    @Valid
    private TargetConfiguration targetConfiguration;
    
    @NotNull(message = "creativeConfiguration is required")
    @Valid
    private CreativeConfiguration creativeConfiguration;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CampaignObjective getObjective() { return objective; }
    public void setObjective(CampaignObjective objective) { this.objective = objective; }
    public BillingModel getBillingModel() { return billingModel; }
    public void setBillingModel(BillingModel billingModel) { this.billingModel = billingModel; }
    public BigDecimal getTotalBudget() { return totalBudget; }
    public void setTotalBudget(BigDecimal totalBudget) { this.totalBudget = totalBudget; }
    public BigDecimal getDailyBudget() { return dailyBudget; }
    public void setDailyBudget(BigDecimal dailyBudget) { this.dailyBudget = dailyBudget; }
    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public TargetConfiguration getTargetConfiguration() { return targetConfiguration; }
    public void setTargetConfiguration(TargetConfiguration targetConfiguration) { this.targetConfiguration = targetConfiguration; }
    public CreativeConfiguration getCreativeConfiguration() { return creativeConfiguration; }
    public void setCreativeConfiguration(CreativeConfiguration creativeConfiguration) { this.creativeConfiguration = creativeConfiguration; }
}
