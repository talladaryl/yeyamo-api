package com.yeyamo_mobile.api.campaign_service.infrastructure.persistence;

import com.yeyamo_mobile.api.campaign_service.domain.model.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
public class CampaignEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "partner_id", nullable = false, length = 100)
    private String partnerId;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CampaignObjective objective;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "promoted_entity_type", nullable = false, length = 50)
    private PromotedEntityType promotedEntityType;
    
    @Column(name = "promoted_entity_id", nullable = false, length = 100)
    private String promotedEntityId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CampaignStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_model", nullable = false, length = 50)
    private BillingModel billingModel;
    
    @Column(name = "total_budget", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalBudget;
    
    @Column(name = "daily_budget", nullable = false, precision = 19, scale = 4)
    private BigDecimal dailyBudget;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Column(name = "start_at", nullable = false)
    private Instant startAt;
    
    @Column(name = "end_at", nullable = false)
    private Instant endAt;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_configuration", nullable = false, columnDefinition = "jsonb")
    private TargetConfiguration targetConfiguration;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "creative_configuration", nullable = false, columnDefinition = "jsonb")
    private CreativeConfiguration creativeConfiguration;
    
    @Column(name = "spent_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal spentAmount;
    
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    
    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    private long version;

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
