package com.yeyamo_mobile.api.ads_delivery_service.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Read-only projection of campaign data optimized for ad selection
 */
public class CampaignProjection {
    private final String campaignId;
    private final String partnerId;
    private final String name;
    private final String objective;
    private final PromotedEntityType promotedEntityType;
    private final String promotedEntityId;
    private final String billingModel;
    private final BigDecimal bidAmount;
    private final BigDecimal totalBudget;
    private final BigDecimal dailyBudget;
    private final BigDecimal spentAmount;
    private final String currency;
    private final Instant startAt;
    private final Instant endAt;
    private final List<PlacementType> eligiblePlacements;
    private final List<String> targetCountries;
    private final List<String> targetRegions;
    private final List<String> targetCities;
    private final List<String> targetInterests;
    private final List<String> targetCategories;
    private final String minAge;
    private final String maxAge;
    private final List<String> targetLanguages;
    private final String creativeJson;
    private final int qualityScore;
    private final Instant createdAt;
    private final String deliveryPolicyVersion;

    private CampaignProjection(Builder builder) {
        this.campaignId = Objects.requireNonNull(builder.campaignId);
        this.partnerId = Objects.requireNonNull(builder.partnerId);
        this.name = Objects.requireNonNull(builder.name);
        this.objective = Objects.requireNonNull(builder.objective);
        this.promotedEntityType = Objects.requireNonNull(builder.promotedEntityType);
        this.promotedEntityId = Objects.requireNonNull(builder.promotedEntityId);
        this.billingModel = Objects.requireNonNull(builder.billingModel);
        this.bidAmount = Objects.requireNonNull(builder.bidAmount);
        this.totalBudget = Objects.requireNonNull(builder.totalBudget);
        this.dailyBudget = Objects.requireNonNull(builder.dailyBudget);
        this.spentAmount = Objects.requireNonNull(builder.spentAmount);
        this.currency = Objects.requireNonNull(builder.currency);
        this.startAt = Objects.requireNonNull(builder.startAt);
        this.endAt = Objects.requireNonNull(builder.endAt);
        this.eligiblePlacements = builder.eligiblePlacements != null ? List.copyOf(builder.eligiblePlacements) : List.of();
        this.targetCountries = builder.targetCountries != null ? List.copyOf(builder.targetCountries) : List.of();
        this.targetRegions = builder.targetRegions != null ? List.copyOf(builder.targetRegions) : List.of();
        this.targetCities = builder.targetCities != null ? List.copyOf(builder.targetCities) : List.of();
        this.targetInterests = builder.targetInterests != null ? List.copyOf(builder.targetInterests) : List.of();
        this.targetCategories = builder.targetCategories != null ? List.copyOf(builder.targetCategories) : List.of();
        this.minAge = builder.minAge;
        this.maxAge = builder.maxAge;
        this.targetLanguages = builder.targetLanguages != null ? List.copyOf(builder.targetLanguages) : List.of();
        this.creativeJson = Objects.requireNonNull(builder.creativeJson);
        this.qualityScore = builder.qualityScore;
        this.createdAt = Objects.requireNonNull(builder.createdAt);
        this.deliveryPolicyVersion = builder.deliveryPolicyVersion != null ? builder.deliveryPolicyVersion : "1.0";
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isActive(Instant now) {
        return !now.isBefore(startAt) && !now.isAfter(endAt);
    }

    public boolean hasBudget() {
        return spentAmount.compareTo(totalBudget) < 0;
    }

    public boolean matchesPlacement(PlacementType placement) {
        return eligiblePlacements.isEmpty() || eligiblePlacements.contains(placement);
    }

    public BigDecimal getRemainingBudget() {
        return totalBudget.subtract(spentAmount);
    }

    // Getters
    public String getCampaignId() { return campaignId; }
    public String getPartnerId() { return partnerId; }
    public String getName() { return name; }
    public String getObjective() { return objective; }
    public PromotedEntityType getPromotedEntityType() { return promotedEntityType; }
    public String getPromotedEntityId() { return promotedEntityId; }
    public String getBillingModel() { return billingModel; }
    public BigDecimal getBidAmount() { return bidAmount; }
    public BigDecimal getTotalBudget() { return totalBudget; }
    public BigDecimal getDailyBudget() { return dailyBudget; }
    public BigDecimal getSpentAmount() { return spentAmount; }
    public String getCurrency() { return currency; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public List<PlacementType> getEligiblePlacements() { return eligiblePlacements; }
    public List<String> getTargetCountries() { return targetCountries; }
    public List<String> getTargetRegions() { return targetRegions; }
    public List<String> getTargetCities() { return targetCities; }
    public List<String> getTargetInterests() { return targetInterests; }
    public List<String> getTargetCategories() { return targetCategories; }
    public String getMinAge() { return minAge; }
    public String getMaxAge() { return maxAge; }
    public List<String> getTargetLanguages() { return targetLanguages; }
    public String getCreativeJson() { return creativeJson; }
    public int getQualityScore() { return qualityScore; }
    public Instant getCreatedAt() { return createdAt; }
    public String getDeliveryPolicyVersion() { return deliveryPolicyVersion; }

    public static class Builder {
        private String campaignId;
        private String partnerId;
        private String name;
        private String objective;
        private PromotedEntityType promotedEntityType;
        private String promotedEntityId;
        private String billingModel;
        private BigDecimal bidAmount;
        private BigDecimal totalBudget;
        private BigDecimal dailyBudget;
        private BigDecimal spentAmount;
        private String currency;
        private Instant startAt;
        private Instant endAt;
        private List<PlacementType> eligiblePlacements;
        private List<String> targetCountries;
        private List<String> targetRegions;
        private List<String> targetCities;
        private List<String> targetInterests;
        private List<String> targetCategories;
        private String minAge;
        private String maxAge;
        private List<String> targetLanguages;
        private String creativeJson;
        private int qualityScore;
        private Instant createdAt;
        private String deliveryPolicyVersion;

        public Builder campaignId(String campaignId) { this.campaignId = campaignId; return this; }
        public Builder partnerId(String partnerId) { this.partnerId = partnerId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder objective(String objective) { this.objective = objective; return this; }
        public Builder promotedEntityType(PromotedEntityType promotedEntityType) { this.promotedEntityType = promotedEntityType; return this; }
        public Builder promotedEntityId(String promotedEntityId) { this.promotedEntityId = promotedEntityId; return this; }
        public Builder billingModel(String billingModel) { this.billingModel = billingModel; return this; }
        public Builder bidAmount(BigDecimal bidAmount) { this.bidAmount = bidAmount; return this; }
        public Builder totalBudget(BigDecimal totalBudget) { this.totalBudget = totalBudget; return this; }
        public Builder dailyBudget(BigDecimal dailyBudget) { this.dailyBudget = dailyBudget; return this; }
        public Builder spentAmount(BigDecimal spentAmount) { this.spentAmount = spentAmount; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder startAt(Instant startAt) { this.startAt = startAt; return this; }
        public Builder endAt(Instant endAt) { this.endAt = endAt; return this; }
        public Builder eligiblePlacements(List<PlacementType> eligiblePlacements) { this.eligiblePlacements = eligiblePlacements; return this; }
        public Builder targetCountries(List<String> targetCountries) { this.targetCountries = targetCountries; return this; }
        public Builder targetRegions(List<String> targetRegions) { this.targetRegions = targetRegions; return this; }
        public Builder targetCities(List<String> targetCities) { this.targetCities = targetCities; return this; }
        public Builder targetInterests(List<String> targetInterests) { this.targetInterests = targetInterests; return this; }
        public Builder targetCategories(List<String> targetCategories) { this.targetCategories = targetCategories; return this; }
        public Builder minAge(String minAge) { this.minAge = minAge; return this; }
        public Builder maxAge(String maxAge) { this.maxAge = maxAge; return this; }
        public Builder targetLanguages(List<String> targetLanguages) { this.targetLanguages = targetLanguages; return this; }
        public Builder creativeJson(String creativeJson) { this.creativeJson = creativeJson; return this; }
        public Builder qualityScore(int qualityScore) { this.qualityScore = qualityScore; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder deliveryPolicyVersion(String deliveryPolicyVersion) { this.deliveryPolicyVersion = deliveryPolicyVersion; return this; }

        public CampaignProjection build() {
            return new CampaignProjection(this);
        }
    }
}
