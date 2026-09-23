package com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.yeyamo_mobile.api.recommendation_service.application.adventure.AdventureAvailabilityStatus;
import com.yeyamo_mobile.api.recommendation_service.application.adventure.AdventureTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "adventure_plan_items")
public class AdventurePlanItemEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "day_id", nullable = false) private AdventurePlanDayEntity day;
    @Column(name = "recommendation_id", nullable = false, unique = true) private UUID recommendationId;
    @Column(name = "source_id", nullable = false, length = 160) private String sourceId;
    @Enumerated(EnumType.STRING) @Column(name = "target_type", nullable = false, length = 30) private AdventureTargetType targetType;
    @Column(name = "target_id", nullable = false, length = 120) private String targetId;
    @Column(name = "scheduled_at") private Instant scheduledAt;
    @Column(nullable = false) private int position;
    @Column(name = "snapshot_title", nullable = false, length = 300) private String snapshotTitle;
    @Column(name = "image_media_id") private UUID imageMediaId;
    @Column(name = "location_label", length = 300) private String locationLabel;
    @Column(name = "starts_at") private Instant startsAt;
    @Column(name = "ends_at") private Instant endsAt;
    @Column(precision = 19, scale = 2) private BigDecimal price;
    @Column(name = "currency_code", length = 3) private String currencyCode;
    @Enumerated(EnumType.STRING) @Column(name = "availability_status", nullable = false, length = 20) private AdventureAvailabilityStatus availabilityStatus;
    @Column(name = "reason_codes", length = 500) private String reasonCodes;
    @Column(name = "skipped_at") private Instant skippedAt;
    @Column(name = "replaced_by_recommendation_id") private UUID replacedByRecommendationId;

    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public AdventurePlanDayEntity getDay() { return day; } public void setDay(AdventurePlanDayEntity day) { this.day = day; }
    public UUID getRecommendationId() { return recommendationId; } public void setRecommendationId(UUID id) { recommendationId = id; }
    public String getSourceId() { return sourceId; } public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public AdventureTargetType getTargetType() { return targetType; } public void setTargetType(AdventureTargetType type) { targetType = type; }
    public String getTargetId() { return targetId; } public void setTargetId(String id) { targetId = id; }
    public Instant getScheduledAt() { return scheduledAt; } public void setScheduledAt(Instant value) { scheduledAt = value; }
    public int getPosition() { return position; } public void setPosition(int position) { this.position = position; }
    public String getSnapshotTitle() { return snapshotTitle; } public void setSnapshotTitle(String value) { snapshotTitle = value; }
    public UUID getImageMediaId() { return imageMediaId; } public void setImageMediaId(UUID value) { imageMediaId = value; }
    public String getLocationLabel() { return locationLabel; } public void setLocationLabel(String value) { locationLabel = value; }
    public Instant getStartsAt() { return startsAt; } public void setStartsAt(Instant value) { startsAt = value; }
    public Instant getEndsAt() { return endsAt; } public void setEndsAt(Instant value) { endsAt = value; }
    public BigDecimal getPrice() { return price; } public void setPrice(BigDecimal value) { price = value; }
    public String getCurrencyCode() { return currencyCode; } public void setCurrencyCode(String value) { currencyCode = value; }
    public AdventureAvailabilityStatus getAvailabilityStatus() { return availabilityStatus; } public void setAvailabilityStatus(AdventureAvailabilityStatus value) { availabilityStatus = value; }
    public String getReasonCodes() { return reasonCodes; } public void setReasonCodes(String value) { reasonCodes = value; }
    public Instant getSkippedAt() { return skippedAt; } public void setSkippedAt(Instant value) { skippedAt = value; }
    public UUID getReplacedByRecommendationId() { return replacedByRecommendationId; } public void setReplacedByRecommendationId(UUID value) { replacedByRecommendationId = value; }
}
