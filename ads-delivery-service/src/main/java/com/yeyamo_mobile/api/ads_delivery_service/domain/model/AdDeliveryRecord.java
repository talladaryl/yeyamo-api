package com.yeyamo_mobile.api.ads_delivery_service.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain aggregate for tracking ad delivery lifecycle
 */
public class AdDeliveryRecord {
    private final String deliveryId;
    private final String campaignId;
    private final String userId;
    private final Instant impressionAt;
    private final Long viewDurationMs;
    private Instant clickedAt;
    private Instant convertedAt;
    private String conversionType;
    private BigDecimal conversionValue;
    private final Instant createdAt;

    private AdDeliveryRecord(
            String deliveryId,
            String campaignId,
            String userId,
            Instant impressionAt,
            Long viewDurationMs) {
        this.deliveryId = Objects.requireNonNull(deliveryId, "deliveryId cannot be null");
        this.campaignId = Objects.requireNonNull(campaignId, "campaignId cannot be null");
        this.userId = userId;
        this.impressionAt = Objects.requireNonNull(impressionAt, "impressionAt cannot be null");
        this.viewDurationMs = viewDurationMs;
        this.createdAt = Instant.now();
    }

    public static AdDeliveryRecord createImpression(
            String deliveryId,
            String campaignId,
            String userId,
            Instant impressionAt,
            Long viewDurationMs) {
        return new AdDeliveryRecord(deliveryId, campaignId, userId, impressionAt, viewDurationMs);
    }

    public void recordClick(Instant clickedAt) {
        if (this.clickedAt != null) {
            throw new IllegalStateException("Click already recorded for delivery: " + deliveryId);
        }
        this.clickedAt = Objects.requireNonNull(clickedAt, "clickedAt cannot be null");
    }

    public void recordConversion(Instant convertedAt, String conversionType, BigDecimal conversionValue) {
        if (this.convertedAt != null) {
            throw new IllegalStateException("Conversion already recorded for delivery: " + deliveryId);
        }
        this.convertedAt = Objects.requireNonNull(convertedAt, "convertedAt cannot be null");
        this.conversionType = Objects.requireNonNull(conversionType, "conversionType cannot be null");
        this.conversionValue = conversionValue;
    }

    public boolean hasClick() {
        return clickedAt != null;
    }

    public boolean hasConversion() {
        return convertedAt != null;
    }

    public boolean isQualifiedImpression() {
        // Qualified impression = viewed for at least 1 second
        return viewDurationMs != null && viewDurationMs >= 1000;
    }

    // Getters
    public String getDeliveryId() { return deliveryId; }
    public String getCampaignId() { return campaignId; }
    public String getUserId() { return userId; }
    public Instant getImpressionAt() { return impressionAt; }
    public Long getViewDurationMs() { return viewDurationMs; }
    public Instant getClickedAt() { return clickedAt; }
    public Instant getConvertedAt() { return convertedAt; }
    public String getConversionType() { return conversionType; }
    public BigDecimal getConversionValue() { return conversionValue; }
    public Instant getCreatedAt() { return createdAt; }
}
