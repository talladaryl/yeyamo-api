package com.yeyamo_mobile.api.ads_delivery_service.domain.service;

import com.yeyamo_mobile.api.ads_delivery_service.domain.model.AdSelectionContext;
import com.yeyamo_mobile.api.ads_delivery_service.domain.model.CampaignProjection;
import com.yeyamo_mobile.api.ads_delivery_service.domain.model.ScoredAd;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Domain service for ad selection and scoring
 */
public class AdSelectionEngine {
    private static final String POLICY_VERSION = "1.0";
    private static final double EXPLORATION_RATE = 0.05;
    private final Random random;

    public AdSelectionEngine() {
        this(new Random());
    }

    AdSelectionEngine(Random random) {
        this.random = random;
    }

    public List<ScoredAd> selectAds(List<CampaignProjection> eligibleCampaigns, AdSelectionContext context) {
        List<ScoredAd> scoredAds = new ArrayList<>();

        for (CampaignProjection campaign : eligibleCampaigns) {
            if (!isEligible(campaign, context)) {
                continue;
            }

            double score = calculateScore(campaign, context);
            List<String> reasonCodes = generateReasonCodes(campaign, context);
            
            scoredAds.add(new ScoredAd(campaign, score, reasonCodes, POLICY_VERSION));
        }

        // Sort by score descending
        scoredAds.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // Apply exploration
        if (random.nextDouble() < EXPLORATION_RATE && scoredAds.size() > 1) {
            int randomIndex = random.nextInt(Math.min(5, scoredAds.size()));
            if (randomIndex > 0) {
                ScoredAd explored = scoredAds.remove(randomIndex);
                scoredAds.add(0, explored);
            }
        }

        return scoredAds.subList(0, Math.min(context.getLimit(), scoredAds.size()));
    }

    private boolean isEligible(CampaignProjection campaign, AdSelectionContext context) {
        // Active period check
        if (!campaign.isActive(context.getRequestTimestamp())) {
            return false;
        }

        // Budget check
        if (!campaign.hasBudget()) {
            return false;
        }

        // Placement check
        if (!campaign.matchesPlacement(context.getPlacement())) {
            return false;
        }

        // Geographic targeting
        if (!matchesGeography(campaign, context)) {
            return false;
        }

        // Language targeting
        if (!campaign.getTargetLanguages().isEmpty() && 
            !campaign.getTargetLanguages().contains(context.getLanguage())) {
            return false;
        }

        // Excluded campaigns
        if (context.getExcludedCampaignIds().contains(campaign.getCampaignId())) {
            return false;
        }

        return true;
    }

    private boolean matchesGeography(CampaignProjection campaign, AdSelectionContext context) {
        // Country match
        if (!campaign.getTargetCountries().isEmpty() && 
            !campaign.getTargetCountries().contains(context.getCountryCode())) {
            return false;
        }

        // Region match
        if (!campaign.getTargetRegions().isEmpty() && context.getRegionId() != null &&
            !campaign.getTargetRegions().contains(context.getRegionId())) {
            return false;
        }

        // City match
        if (!campaign.getTargetCities().isEmpty() && context.getCityId() != null &&
            !campaign.getTargetCities().contains(context.getCityId())) {
            return false;
        }

        return true;
    }

    private double calculateScore(CampaignProjection campaign, AdSelectionContext context) {
        double bidScore = normalizeBid(campaign.getBidAmount().doubleValue());
        double geoRelevance = calculateGeographicRelevance(campaign, context);
        double interestAffinity = calculateInterestAffinity(campaign, context);
        double contextualRelevance = calculateContextualRelevance(campaign, context);
        double qualityScore = campaign.getQualityScore() / 100.0;
        double freshnessScore = calculateFreshnessScore(campaign, context);

        return bidScore * 0.30 +
               geoRelevance * 0.20 +
               interestAffinity * 0.20 +
               contextualRelevance * 0.15 +
               qualityScore * 0.10 +
               freshnessScore * 0.05;
    }

    private double normalizeBid(double bidAmount) {
        // Normalize bid to 0-1 range (assuming max bid is 10000 XAF)
        return Math.min(bidAmount / 10000.0, 1.0);
    }

    private double calculateGeographicRelevance(CampaignProjection campaign, AdSelectionContext context) {
        double score = 0.5; // Base score

        if (!campaign.getTargetCities().isEmpty() && context.getCityId() != null &&
            campaign.getTargetCities().contains(context.getCityId())) {
            score = 1.0; // Exact city match
        } else if (!campaign.getTargetRegions().isEmpty() && context.getRegionId() != null &&
                   campaign.getTargetRegions().contains(context.getRegionId())) {
            score = 0.8; // Region match
        } else if (!campaign.getTargetCountries().isEmpty() &&
                   campaign.getTargetCountries().contains(context.getCountryCode())) {
            score = 0.6; // Country match
        }

        return score;
    }

    private double calculateInterestAffinity(CampaignProjection campaign, AdSelectionContext context) {
        if (campaign.getTargetInterests().isEmpty() || context.getInterestIds().isEmpty()) {
            return 0.5; // Neutral score if no interest targeting
        }

        long matchingInterests = context.getInterestIds().stream()
            .filter(campaign.getTargetInterests()::contains)
            .count();

        return Math.min(matchingInterests / (double) campaign.getTargetInterests().size(), 1.0);
    }

    private double calculateContextualRelevance(CampaignProjection campaign, AdSelectionContext context) {
        if (context.getContextEntityType() == null) {
            return 0.5; // Neutral if no context
        }

        // Boost if promoted entity type matches context
        if (campaign.getPromotedEntityType() == context.getContextEntityType()) {
            return 1.0;
        }

        return 0.3;
    }

    private double calculateFreshnessScore(CampaignProjection campaign, AdSelectionContext context) {
        long ageInDays = java.time.Duration.between(campaign.getCreatedAt(), context.getRequestTimestamp()).toDays();
        
        if (ageInDays <= 1) return 1.0;
        if (ageInDays <= 7) return 0.8;
        if (ageInDays <= 30) return 0.6;
        return 0.4;
    }

    private List<String> generateReasonCodes(CampaignProjection campaign, AdSelectionContext context) {
        List<String> reasons = new ArrayList<>();

        // Geographic relevance
        if (context.getCityId() != null && campaign.getTargetCities().contains(context.getCityId())) {
            reasons.add("LOCAL_MATCH");
        } else if (context.getRegionId() != null && campaign.getTargetRegions().contains(context.getRegionId())) {
            reasons.add("REGIONAL_MATCH");
        }

        // Interest match
        long matchingInterests = context.getInterestIds().stream()
            .filter(campaign.getTargetInterests()::contains)
            .count();
        if (matchingInterests > 0) {
            reasons.add("INTEREST_MATCH");
        }

        // Context match
        if (context.getContextEntityType() == campaign.getPromotedEntityType()) {
            reasons.add("CONTEXTUAL_MATCH");
        }

        // Quality
        if (campaign.getQualityScore() >= 80) {
            reasons.add("HIGH_QUALITY");
        }

        // Freshness
        long ageInDays = java.time.Duration.between(campaign.getCreatedAt(), context.getRequestTimestamp()).toDays();
        if (ageInDays <= 7) {
            reasons.add("NEW_CAMPAIGN");
        }

        if (reasons.isEmpty()) {
            reasons.add("GENERAL_RELEVANCE");
        }

        return reasons;
    }
}
