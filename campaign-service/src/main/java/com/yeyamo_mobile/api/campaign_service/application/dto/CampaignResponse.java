package com.yeyamo_mobile.api.campaign_service.application.dto;

import com.yeyamo_mobile.api.campaign_service.domain.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CampaignResponse {
    private UUID id;
    private String partnerId;
    private String name;
    private CampaignObjective objective;
    private PromotedEntityType promotedEntityType;
    private String promotedEntityId;
    private CampaignStatus status;
    private BillingModel billingModel;
    private BigDecimal totalBudget;
    private BigDecimal dailyBudget;
    private String currency;
    private Instant startAt;
    private Instant endAt;
    private TargetConfiguration targetConfiguration;
    private CreativeConfiguration creativeConfiguration;
    private BigDecimal spentAmount;
    private String createdBy;
    private String approvedBy;
    private String rejectionReason;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    public static CampaignResponse from(Campaign campaign) {
        CampaignResponse response = new CampaignResponse();
        response.id = campaign.getId();
        response.partnerId = campaign.getPartnerId();
        response.name = campaign.getName();
        response.objective = campaign.getObjective();
        response.promotedEntityType = campaign.getPromotedEntityType();
        response.promotedEntityId = campaign.getPromotedEntityId();
        response.status = campaign.getStatus();
        response.billingModel = campaign.getBillingModel();
        response.totalBudget = campaign.getTotalBudget();
        response.dailyBudget = campaign.getDailyBudget();
        response.currency = campaign.getCurrency();
        response.startAt = campaign.getStartAt();
        response.endAt = campaign.getEndAt();
        response.targetConfiguration = campaign.getTargetConfiguration();
        response.creativeConfiguration = campaign.getCreativeConfiguration();
        response.spentAmount = campaign.getSpentAmount();
        response.createdBy = campaign.getCreatedBy();
        response.approvedBy = campaign.getApprovedBy();
        response.rejectionReason = campaign.getRejectionReason();
        response.createdAt = campaign.getCreatedAt();
        response.updatedAt = campaign.getUpdatedAt();
        response.version = campaign.getVersion();
        return response;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CampaignObjective getObjective() { return objective; }
    public void setObjective(CampaignObjective objective) { this.objective = objective; }
    public PromotedEntityType getPromotedEntityType() { return promotedEntityType; }
    public void setPromotedEntityType(PromotedEntityType promotedEntityType) { this.promotedEntityType = promotedEntityType; }
    public String getPromotedEntityId() { return promotedEntityId; }
    public void setPromotedEntityId(String promotedEntityId) { this.promotedEntityId = promotedEntityId; }
    public CampaignStatus getStatus() { return status; }
    public void setStatus(CampaignStatus status) { this.status = status; }
    public BillingModel getBillingModel() { return billingModel; }
    public void setBillingModel(BillingModel billingModel) { this.billingModel = billingModel; }
    public BigDecimal getTotalBudget() { return totalBudget; }
    public void setTotalBudget(BigDecimal totalBudget) { this.totalBudget = totalBudget; }
    public BigDecimal getDailyBudget() { return dailyBudget; }
    public void setDailyBudget(BigDecimal dailyBudget) { this.dailyBudget = dailyBudget; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public TargetConfiguration getTargetConfiguration() { return targetConfiguration; }
    public void setTargetConfiguration(TargetConfiguration targetConfiguration) { this.targetConfiguration = targetConfiguration; }
    public CreativeConfiguration getCreativeConfiguration() { return creativeConfiguration; }
    public void setCreativeConfiguration(CreativeConfiguration creativeConfiguration) { this.creativeConfiguration = creativeConfiguration; }
    public BigDecimal getSpentAmount() { return spentAmount; }
    public void setSpentAmount(BigDecimal spentAmount) { this.spentAmount = spentAmount; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
