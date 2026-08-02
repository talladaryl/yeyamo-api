package com.yeyamo_mobile.api.campaign_service.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Campaign Aggregate Root - Pure domain model (no Spring/JPA dependencies)
 */
public class Campaign {
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

    /**
     * Factory method to create a new campaign in DRAFT status
     */
    public static Campaign create(
            String partnerId,
            String name,
            CampaignObjective objective,
            PromotedEntityType promotedEntityType,
            String promotedEntityId,
            BillingModel billingModel,
            BigDecimal totalBudget,
            BigDecimal dailyBudget,
            String currency,
            Instant startAt,
            Instant endAt,
            TargetConfiguration targetConfiguration,
            CreativeConfiguration creativeConfiguration,
            String createdBy) {
        
        Instant now = Instant.now();
        Campaign campaign = new Campaign();
        campaign.id = UUID.randomUUID();
        campaign.partnerId = require(partnerId, "partnerId");
        campaign.name = require(name, "name");
        campaign.objective = requireNotNull(objective, "objective");
        campaign.promotedEntityType = requireNotNull(promotedEntityType, "promotedEntityType");
        campaign.promotedEntityId = require(promotedEntityId, "promotedEntityId");
        campaign.billingModel = requireNotNull(billingModel, "billingModel");
        campaign.totalBudget = requirePositive(totalBudget, "totalBudget");
        campaign.dailyBudget = requirePositive(dailyBudget, "dailyBudget");
        campaign.currency = requireNotNull(currency, "currency");
        campaign.startAt = requireNotNull(startAt, "startAt");
        campaign.endAt = requireNotNull(endAt, "endAt");
        campaign.targetConfiguration = requireNotNull(targetConfiguration, "targetConfiguration");
        campaign.creativeConfiguration = requireNotNull(creativeConfiguration, "creativeConfiguration");
        campaign.createdBy = require(createdBy, "createdBy");
        campaign.status = CampaignStatus.DRAFT;
        campaign.spentAmount = BigDecimal.ZERO;
        campaign.createdAt = now;
        campaign.updatedAt = now;
        
        campaign.validateBusinessRules();
        targetConfiguration.validate();
        creativeConfiguration.validate();
        
        return campaign;
    }

    /**
     * Update campaign - only allowed in DRAFT status
     */
    public void update(
            String name,
            CampaignObjective objective,
            BillingModel billingModel,
            BigDecimal totalBudget,
            BigDecimal dailyBudget,
            Instant startAt,
            Instant endAt,
            TargetConfiguration targetConfiguration,
            CreativeConfiguration creativeConfiguration) {
        
        assertCanModify();
        
        this.name = require(name, "name");
        this.objective = requireNotNull(objective, "objective");
        this.billingModel = requireNotNull(billingModel, "billingModel");
        this.totalBudget = requirePositive(totalBudget, "totalBudget");
        this.dailyBudget = requirePositive(dailyBudget, "dailyBudget");
        this.startAt = requireNotNull(startAt, "startAt");
        this.endAt = requireNotNull(endAt, "endAt");
        this.targetConfiguration = requireNotNull(targetConfiguration, "targetConfiguration");
        this.creativeConfiguration = requireNotNull(creativeConfiguration, "creativeConfiguration");
        this.updatedAt = Instant.now();
        
        validateBusinessRules();
        targetConfiguration.validate();
        creativeConfiguration.validate();
    }

    /**
     * Submit campaign for review
     */
    public void submit() {
        if (status != CampaignStatus.DRAFT && status != CampaignStatus.REJECTED) {
            throw new IllegalStateException("Can only submit campaigns in DRAFT or REJECTED status");
        }
        this.status = CampaignStatus.PENDING_REVIEW;
        this.rejectionReason = null;
        this.updatedAt = Instant.now();
    }

    /**
     * Approve campaign - admin action
     */
    public void approve(String approvedBy) {
        if (status != CampaignStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Can only approve campaigns in PENDING_REVIEW status");
        }
        this.status = CampaignStatus.APPROVED;
        this.approvedBy = require(approvedBy, "approvedBy");
        this.rejectionReason = null;
        this.updatedAt = Instant.now();
    }

    /**
     * Reject campaign - admin action
     */
    public void reject(String rejectionReason) {
        if (status != CampaignStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Can only reject campaigns in PENDING_REVIEW status");
        }
        this.status = CampaignStatus.REJECTED;
        this.rejectionReason = require(rejectionReason, "rejectionReason");
        this.updatedAt = Instant.now();
    }


    /**
     * Activate campaign - must be APPROVED and not expired
     */
    public void activate() {
        if (status != CampaignStatus.APPROVED && status != CampaignStatus.PAUSED) {
            throw new IllegalStateException("Can only activate campaigns in APPROVED or PAUSED status");
        }
        if (isExpired()) {
            throw new IllegalStateException("Cannot activate expired campaign");
        }
        this.status = CampaignStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /**
     * Pause active campaign
     */
    public void pause() {
        if (status != CampaignStatus.ACTIVE) {
            throw new IllegalStateException("Can only pause ACTIVE campaigns");
        }
        this.status = CampaignStatus.PAUSED;
        this.updatedAt = Instant.now();
    }

    /**
     * Resume paused campaign
     */
    public void resume() {
        if (status != CampaignStatus.PAUSED) {
            throw new IllegalStateException("Can only resume PAUSED campaigns");
        }
        if (isExpired()) {
            throw new IllegalStateException("Cannot resume expired campaign");
        }
        this.status = CampaignStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /**
     * Cancel campaign
     */
    public void cancel() {
        if (status == CampaignStatus.COMPLETED || status == CampaignStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel COMPLETED or CANCELLED campaigns");
        }
        this.status = CampaignStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    /**
     * Complete campaign - when end date reached or manually completed
     */
    public void complete() {
        if (status != CampaignStatus.ACTIVE && status != CampaignStatus.PAUSED && status != CampaignStatus.BUDGET_EXHAUSTED) {
            throw new IllegalStateException("Can only complete ACTIVE, PAUSED, or BUDGET_EXHAUSTED campaigns");
        }
        this.status = CampaignStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    /**
     * Deduct budget - atomic operation
     */
    public void deductBudget(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deduction amount must be positive");
        }
        
        BigDecimal newSpentAmount = this.spentAmount.add(amount);
        if (newSpentAmount.compareTo(this.totalBudget) > 0) {
            throw new IllegalStateException("Cannot exceed total budget. Spent: " + newSpentAmount + ", Total: " + this.totalBudget);
        }
        
        this.spentAmount = newSpentAmount;
        this.updatedAt = Instant.now();
        
        // Auto-transition to BUDGET_EXHAUSTED if budget is now exhausted
        if (isBudgetExhausted() && status == CampaignStatus.ACTIVE) {
            this.status = CampaignStatus.BUDGET_EXHAUSTED;
        }
    }

    /**
     * Check if budget is exhausted
     */
    public boolean isBudgetExhausted() {
        return spentAmount.compareTo(totalBudget) >= 0;
    }

    /**
     * Check if campaign is expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(endAt);
    }

    /**
     * Check if campaign can be modified
     */
    private void assertCanModify() {
        if (status == CampaignStatus.ACTIVE) {
            throw new IllegalStateException("Cannot modify ACTIVE campaigns");
        }
        if (status == CampaignStatus.COMPLETED || status == CampaignStatus.CANCELLED) {
            throw new IllegalStateException("Cannot modify COMPLETED or CANCELLED campaigns");
        }
        if (status == CampaignStatus.PENDING_REVIEW || status == CampaignStatus.APPROVED) {
            throw new IllegalStateException("Cannot modify campaigns in PENDING_REVIEW or APPROVED status");
        }
    }

    /**
     * Validate all business rules
     */
    private void validateBusinessRules() {
        // totalBudget > 0
        if (totalBudget.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("totalBudget must be positive");
        }
        
        // dailyBudget > 0 and <= totalBudget
        if (dailyBudget.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("dailyBudget must be positive");
        }
        if (dailyBudget.compareTo(totalBudget) > 0) {
            throw new IllegalArgumentException("dailyBudget cannot exceed totalBudget");
        }
        
        // endAt must be after startAt
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }
        
        // spentAmount cannot be negative
        if (spentAmount != null && spentAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("spentAmount cannot be negative");
        }
    }

    // Validation helpers
    private static String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static <T> T requireNotNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
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
