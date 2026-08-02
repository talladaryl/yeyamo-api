package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ad_delivery_records")
public class AdDeliveryRecordEntity {

    @Id
    @Column(name = "delivery_id", length = 100)
    private String deliveryId;

    @Column(name = "campaign_id", nullable = false, length = 100)
    private String campaignId;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "anonymous_session_id", length = 100)
    private String anonymousSessionId;

    @Column(name = "placement", nullable = false, length = 50)
    private String placement;

    @Column(name = "impression_at", nullable = false)
    private Instant impressionAt;

    @Column(name = "view_duration_ms")
    private Long viewDurationMs;

    @Column(name = "clicked_at")
    private Instant clickedAt;

    @Column(name = "converted_at")
    private Instant convertedAt;

    @Column(name = "conversion_type", length = 50)
    private String conversionType;

    @Column(name = "conversion_value", precision = 19, scale = 4)
    private BigDecimal conversionValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Getters and setters
    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }

    public String getCampaignId() { return campaignId; }
    public void setCampaignId(String campaignId) { this.campaignId = campaignId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAnonymousSessionId() { return anonymousSessionId; }
    public void setAnonymousSessionId(String anonymousSessionId) { this.anonymousSessionId = anonymousSessionId; }

    public String getPlacement() { return placement; }
    public void setPlacement(String placement) { this.placement = placement; }

    public Instant getImpressionAt() { return impressionAt; }
    public void setImpressionAt(Instant impressionAt) { this.impressionAt = impressionAt; }

    public Long getViewDurationMs() { return viewDurationMs; }
    public void setViewDurationMs(Long viewDurationMs) { this.viewDurationMs = viewDurationMs; }

    public Instant getClickedAt() { return clickedAt; }
    public void setClickedAt(Instant clickedAt) { this.clickedAt = clickedAt; }

    public Instant getConvertedAt() { return convertedAt; }
    public void setConvertedAt(Instant convertedAt) { this.convertedAt = convertedAt; }

    public String getConversionType() { return conversionType; }
    public void setConversionType(String conversionType) { this.conversionType = conversionType; }

    public BigDecimal getConversionValue() { return conversionValue; }
    public void setConversionValue(BigDecimal conversionValue) { this.conversionValue = conversionValue; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
