package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "campaign_projections", indexes = {
    @Index(name = "idx_campaign_active", columnList = "status,start_at,end_at"),
    @Index(name = "idx_campaign_placement", columnList = "eligible_placements")
})
public class CampaignProjectionEntity {
    
    @Id
    @Column(name = "campaign_id", length = 100)
    private String campaignId;

    @Column(name = "partner_id", nullable = false, length = 100)
    private String partnerId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "objective", nullable = false, length = 50)
    private String objective;

    @Column(name = "promoted_entity_type", nullable = false, length = 50)
    private String promotedEntityType;

    @Column(name = "promoted_entity_id", nullable = false, length = 100)
    private String promotedEntityId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "billing_model", nullable = false, length = 50)
    private String billingModel;

    @Column(name = "bid_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal bidAmount;

    @Column(name = "total_budget", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalBudget;

    @Column(name = "daily_budget", nullable = false, precision = 19, scale = 4)
    private BigDecimal dailyBudget;

    @Column(name = "spent_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal spentAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "eligible_placements", columnDefinition = "TEXT")
    private String eligiblePlacements;

    @Column(name = "target_countries", columnDefinition = "TEXT")
    private String targetCountries;

    @Column(name = "target_regions", columnDefinition = "TEXT")
    private String targetRegions;

    @Column(name = "target_cities", columnDefinition = "TEXT")
    private String targetCities;

    @Column(name = "target_interests", columnDefinition = "TEXT")
    private String targetInterests;

    @Column(name = "target_categories", columnDefinition = "TEXT")
    private String targetCategories;

    @Column(name = "min_age", length = 10)
    private String minAge;

    @Column(name = "max_age", length = 10)
    private String maxAge;

    @Column(name = "target_languages", columnDefinition = "TEXT")
    private String targetLanguages;

    @Column(name = "creative_json", columnDefinition = "TEXT", nullable = false)
    private String creativeJson;

    @Column(name = "quality_score", nullable = false)
    private int qualityScore;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "delivery_policy_version", length = 20)
    private String deliveryPolicyVersion;

    @Version
    @Column(name = "version")
    private Long version;

    // Getters and setters
    public String getCampaignId() { return campaignId; }
    public void setCampaignId(String campaignId) { this.campaignId = campaignId; }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }

    public String getPromotedEntityType() { return promotedEntityType; }
    public void setPromotedEntityType(String promotedEntityType) { this.promotedEntityType = promotedEntityType; }

    public String getPromotedEntityId() { return promotedEntityId; }
    public void setPromotedEntityId(String promotedEntityId) { this.promotedEntityId = promotedEntityId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBillingModel() { return billingModel; }
    public void setBillingModel(String billingModel) { this.billingModel = billingModel; }

    public BigDecimal getBidAmount() { return bidAmount; }
    public void setBidAmount(BigDecimal bidAmount) { this.bidAmount = bidAmount; }

    public BigDecimal getTotalBudget() { return totalBudget; }
    public void setTotalBudget(BigDecimal totalBudget) { this.totalBudget = totalBudget; }

    public BigDecimal getDailyBudget() { return dailyBudget; }
    public void setDailyBudget(BigDecimal dailyBudget) { this.dailyBudget = dailyBudget; }

    public BigDecimal getSpentAmount() { return spentAmount; }
    public void setSpentAmount(BigDecimal spentAmount) { this.spentAmount = spentAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }

    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }

    public String getEligiblePlacements() { return eligiblePlacements; }
    public void setEligiblePlacements(String eligiblePlacements) { this.eligiblePlacements = eligiblePlacements; }

    public String getTargetCountries() { return targetCountries; }
    public void setTargetCountries(String targetCountries) { this.targetCountries = targetCountries; }

    public String getTargetRegions() { return targetRegions; }
    public void setTargetRegions(String targetRegions) { this.targetRegions = targetRegions; }

    public String getTargetCities() { return targetCities; }
    public void setTargetCities(String targetCities) { this.targetCities = targetCities; }

    public String getTargetInterests() { return targetInterests; }
    public void setTargetInterests(String targetInterests) { this.targetInterests = targetInterests; }

    public String getTargetCategories() { return targetCategories; }
    public void setTargetCategories(String targetCategories) { this.targetCategories = targetCategories; }

    public String getMinAge() { return minAge; }
    public void setMinAge(String minAge) { this.minAge = minAge; }

    public String getMaxAge() { return maxAge; }
    public void setMaxAge(String maxAge) { this.maxAge = maxAge; }

    public String getTargetLanguages() { return targetLanguages; }
    public void setTargetLanguages(String targetLanguages) { this.targetLanguages = targetLanguages; }

    public String getCreativeJson() { return creativeJson; }
    public void setCreativeJson(String creativeJson) { this.creativeJson = creativeJson; }

    public int getQualityScore() { return qualityScore; }
    public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getDeliveryPolicyVersion() { return deliveryPolicyVersion; }
    public void setDeliveryPolicyVersion(String deliveryPolicyVersion) { this.deliveryPolicyVersion = deliveryPolicyVersion; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
