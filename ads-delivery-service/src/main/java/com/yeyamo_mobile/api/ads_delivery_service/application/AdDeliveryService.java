package com.yeyamo_mobile.api.ads_delivery_service.application;

import com.yeyamo_mobile.api.ads_delivery_service.application.dto.*;
import com.yeyamo_mobile.api.ads_delivery_service.domain.model.*;
import com.yeyamo_mobile.api.ads_delivery_service.domain.port.*;
import com.yeyamo_mobile.api.ads_delivery_service.domain.service.AdSelectionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import com.yeyamo_mobile.api.ads_delivery_service.infrastructure.kafka.AdsOutboxService;

@Service
public class AdDeliveryService {
    private static final Logger logger = LoggerFactory.getLogger(AdDeliveryService.class);
    private static final String DISCLOSURE_LABEL = "Sponsorisé";
    
    private final CampaignProjectionRepository campaignRepository;
    private final FrequencyCapService frequencyCapService;
    private final BudgetReservationService budgetService;
    private final TrackingTokenService tokenService;
    private final AdDeliveryRecordRepository deliveryRecordRepository;
    private final AdSelectionEngine selectionEngine;
    private final FeatureFlagService featureFlagService;
    private final AdsOutboxService outbox;

    public AdDeliveryService(
            CampaignProjectionRepository campaignRepository,
            FrequencyCapService frequencyCapService,
            BudgetReservationService budgetService,
            TrackingTokenService tokenService,
            AdDeliveryRecordRepository deliveryRecordRepository,
            FeatureFlagService featureFlagService,
            AdsOutboxService outbox) {
        this.campaignRepository = campaignRepository;
        this.frequencyCapService = frequencyCapService;
        this.budgetService = budgetService;
        this.tokenService = tokenService;
        this.deliveryRecordRepository = deliveryRecordRepository;
        this.featureFlagService = featureFlagService;
        this.outbox = outbox;
        this.selectionEngine = new AdSelectionEngine();
    }

    @Transactional(readOnly = true)
    public List<SponsoredPlacementResponse> selectAds(AdSelectionRequest request) {
        // Check feature flags
        if (!featureFlagService.isEnabled("ads_delivery_enabled")) {
            return List.of();
        }

        PlacementType placement = PlacementType.valueOf(request.placement());
        if (!featureFlagService.isPlacementEnabled(placement)) {
            return List.of();
        }

        // Build selection context
        AdSelectionContext context = buildContext(request);

        // Load eligible campaigns
        List<CampaignProjection> eligibleCampaigns = campaignRepository
            .findActiveCampaignsForPlacement(placement, context.getRequestTimestamp());

        if (eligibleCampaigns.isEmpty()) {
            return List.of();
        }

        // Apply frequency caps
        if (context.isAuthenticated()) {
            eligibleCampaigns = eligibleCampaigns.stream()
                .filter(c -> frequencyCapService.canShowAd(context.getUserId(), c.getCampaignId(), "24h"))
                .collect(Collectors.toList());
        }

        // Select and score ads
        List<ScoredAd> scoredAds = selectionEngine.selectAds(eligibleCampaigns, context);

        // Convert to response
        return convertToResponses(scoredAds, context);
    }

    @Transactional
    public void recordImpression(ImpressionRequest request) {
        // Verify token
        if (!tokenService.verifyImpressionToken(request.impressionToken())) {
            throw new IllegalArgumentException("Invalid impression token");
        }

        TrackingTokenData tokenData = tokenService.decodeImpressionToken(request.impressionToken());
        
        // Check idempotency
        if (deliveryRecordRepository.existsByDeliveryId(tokenData.deliveryId())) {
            logger.warn("Duplicate impression attempt for deliveryId: {}", tokenData.deliveryId());
            return;
        }

        CampaignProjection campaign = campaignRepository
            .findById(tokenData.campaignId())
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        BigDecimal charge = impressionCharge(campaign);
        if (!budgetService.reserveBudget(campaign.getCampaignId(), charge,
                campaign.getTotalBudget(), tokenData.deliveryId())) {
            throw new IllegalStateException("Campaign budget exhausted");
        }

        try {
            AdDeliveryRecord record = AdDeliveryRecord.createImpression(
                tokenData.deliveryId(),
                tokenData.campaignId(),
                tokenData.userId(),
                request.viewedAt(),
                request.viewDurationMs()
            );
            deliveryRecordRepository.save(record);
            outbox.append("ad.impression.recorded", campaign.getCampaignId(),
                tokenData.deliveryId(), Map.of(
                    "campaignId", campaign.getCampaignId(),
                    "partnerId", campaign.getPartnerId(),
                    "anonymousId", outbox.anonymousSubject(
                        tokenData.userId() == null
                            ? tokenData.deliveryId() : tokenData.userId()),
                    "qualified", record.isQualifiedImpression(),
                    "cost", charge,
                    "viewDurationMs", request.viewDurationMs()
                ));
            budgetService.confirmReservation(tokenData.deliveryId(), charge);
        } catch (RuntimeException exception) {
            budgetService.releaseReservation(tokenData.deliveryId());
            throw exception;
        }

        // Update frequency cap
        if (tokenData.userId() != null) {
            frequencyCapService.recordImpression(tokenData.userId(), tokenData.campaignId());
        }

        logger.info("Recorded impression for campaign: {}, delivery: {}", 
            tokenData.campaignId(), tokenData.deliveryId());
    }

    private BigDecimal impressionCharge(CampaignProjection campaign) {
        return switch (campaign.getBillingModel()) {
            case "CPM" -> campaign.getBidAmount().divide(
                BigDecimal.valueOf(1000), 4, java.math.RoundingMode.HALF_UP);
            case "FIXED_BUDGET" -> BigDecimal.ZERO;
            default -> BigDecimal.ZERO;
        };
    }

    @Transactional
    public void recordClick(ClickRequest request) {
        // Verify token
        if (!tokenService.verifyClickToken(request.clickToken())) {
            throw new IllegalArgumentException("Invalid click token");
        }

        TrackingTokenData tokenData = tokenService.decodeClickToken(request.clickToken());

        // Find delivery record
        Optional<AdDeliveryRecord> recordOpt = deliveryRecordRepository.findByDeliveryId(tokenData.deliveryId());
        
        if (recordOpt.isEmpty()) {
            logger.warn("Click without impression for deliveryId: {}", tokenData.deliveryId());
            return;
        }

        AdDeliveryRecord record = recordOpt.get();
        
        if (record.hasClick()) {
            logger.warn("Duplicate click for deliveryId: {}", tokenData.deliveryId());
            return;
        }

        CampaignProjection campaign = campaignRepository.findById(record.getCampaignId())
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        reserveAndRun(campaign, tokenData.deliveryId() + ":click",
            "CPC".equals(campaign.getBillingModel())
                ? campaign.getBidAmount() : BigDecimal.ZERO,
            () -> {
                record.recordClick(request.clickedAt());
                deliveryRecordRepository.save(record);
                outbox.append("ad.click.recorded", campaign.getCampaignId(),
                    tokenData.deliveryId(), Map.of(
                        "campaignId", campaign.getCampaignId(),
                        "partnerId", campaign.getPartnerId(),
                        "cost", "CPC".equals(campaign.getBillingModel())
                            ? campaign.getBidAmount() : BigDecimal.ZERO
                    ));
            });

        logger.info("Recorded click for campaign: {}, delivery: {}", 
            tokenData.campaignId(), tokenData.deliveryId());
    }

    @Transactional
    public void recordConversion(ConversionRequest request) {
        AdDeliveryRecord record = deliveryRecordRepository.findByDeliveryId(request.deliveryId())
            .orElseThrow(() -> new IllegalArgumentException("Delivery not found"));

        if (record.hasConversion()) {
            logger.warn("Duplicate conversion for deliveryId: {}", request.deliveryId());
            return;
        }

        CampaignProjection campaign = campaignRepository.findById(record.getCampaignId())
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        reserveAndRun(campaign, request.deliveryId() + ":conversion",
            "CPA".equals(campaign.getBillingModel())
                ? campaign.getBidAmount() : BigDecimal.ZERO,
            () -> {
                record.recordConversion(request.convertedAt(),
                    request.conversionType(), request.conversionValue());
                deliveryRecordRepository.save(record);
                outbox.append("ad.conversion.recorded",
                    campaign.getCampaignId(), request.deliveryId(), Map.of(
                        "campaignId", campaign.getCampaignId(),
                        "partnerId", campaign.getPartnerId(),
                        "conversionType", request.conversionType(),
                        "cost", "CPA".equals(campaign.getBillingModel())
                            ? campaign.getBidAmount() : BigDecimal.ZERO
                    ));
            });

        logger.info("Recorded conversion for campaign: {}, delivery: {}, type: {}", 
            record.getCampaignId(), request.deliveryId(), request.conversionType());
    }

    private void reserveAndRun(CampaignProjection campaign, String reservationId,
            BigDecimal amount, Runnable mutation) {
        if (!budgetService.reserveBudget(campaign.getCampaignId(), amount,
                campaign.getTotalBudget(), reservationId)) {
            throw new IllegalStateException("Campaign budget exhausted");
        }
        try {
            mutation.run();
            budgetService.confirmReservation(reservationId, amount);
        } catch (RuntimeException exception) {
            budgetService.releaseReservation(reservationId);
            throw exception;
        }
    }

    private AdSelectionContext buildContext(AdSelectionRequest request) {
        return AdSelectionContext.builder()
            .userId(request.userId())
            .anonymousSessionId(request.anonymousSessionId())
            .placement(PlacementType.valueOf(request.placement()))
            .countryCode(request.countryCode())
            .regionId(request.regionId())
            .cityId(request.cityId())
            .districtId(request.districtId())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .interestIds(request.interestIds() != null ? request.interestIds() : List.of())
            .ageBand(request.ageBand())
            .language(request.language())
            .deviceType(request.deviceType())
            .requestTimestamp(request.requestTimestamp())
            .contextEntityType(request.contextEntityType() != null ? 
                PromotedEntityType.valueOf(request.contextEntityType()) : null)
            .contextEntityId(request.contextEntityId())
            .excludedCampaignIds(request.excludedCampaignIds() != null ? 
                request.excludedCampaignIds() : List.of())
            .limit(request.limit() != null ? request.limit() : 10)
            .build();
    }

    private List<SponsoredPlacementResponse> convertToResponses(List<ScoredAd> scoredAds, AdSelectionContext context) {
        List<SponsoredPlacementResponse> responses = new ArrayList<>();
        
        for (int i = 0; i < scoredAds.size(); i++) {
            ScoredAd scoredAd = scoredAds.get(i);
            CampaignProjection campaign = scoredAd.getCampaign();
            
            String deliveryId = UUID.randomUUID().toString();
            Instant expiresAt = context.getRequestTimestamp().plusSeconds(300); // 5 minutes
            
            // Generate tracking tokens
            String impressionToken = tokenService.generateImpressionToken(
                deliveryId, 
                campaign.getCampaignId(),
                context.getUserId(),
                expiresAt
            );
            
            String clickToken = tokenService.generateClickToken(
                deliveryId,
                campaign.getCampaignId(),
                context.getUserId(),
                expiresAt
            );

            // Parse creative JSON
            Map<String, Object> creative = parseCreative(campaign.getCreativeJson());

            responses.add(new SponsoredPlacementResponse(
                deliveryId,
                campaign.getCampaignId(),
                campaign.getPromotedEntityType().name(),
                campaign.getPromotedEntityId(),
                context.getPlacement().name(),
                creative,
                DISCLOSURE_LABEL,
                scoredAd.getReasonCodes(),
                impressionToken,
                clickToken,
                expiresAt,
                i + 1,
                scoredAd.getPolicyVersion()
            ));
        }

        return responses;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseCreative(String creativeJson) {
        try {
            // Simple JSON parsing - in production use Jackson
            return Map.of("raw", creativeJson);
        } catch (Exception e) {
            logger.error("Failed to parse creative JSON", e);
            return Map.of();
        }
    }
}
