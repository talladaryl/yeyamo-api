package com.yeyamo_mobile.api.place_service.models;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** A public contribution awaiting moderation; it is not a canonical Place. */
@Entity
@Table(name = "place_suggestions")
public class PlaceSuggestion {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "submitter_user_id", nullable = false, length = 120)
    private String submitterUserId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 255)
    private String normalizedName;

    @Column(length = 500)
    private String address;

    @Column(name = "normalized_address", nullable = false, length = 500)
    private String normalizedAddress;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "category_label", length = 120)
    private String categoryLabel;

    @Column(name = "place_type", length = 120)
    private String placeType;

    @Column(name = "region_label", length = 120)
    private String regionLabel;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "canonical_place_id")
    private UUID canonicalPlaceId;

    @Column(name = "moderation_reason", length = 1000)
    private String moderationReason;

    @Column(name = "reviewed_by", length = 120)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    public static PlaceSuggestion pending(String submitterUserId, String name, String normalizedName, String address,
            String normalizedAddress, String description, String categoryLabel, String placeType, String regionLabel,
            String countryCode, double latitude, double longitude) {
        PlaceSuggestion suggestion = new PlaceSuggestion();
        suggestion.submitterUserId = submitterUserId;
        suggestion.name = name;
        suggestion.normalizedName = normalizedName;
        suggestion.address = address;
        suggestion.normalizedAddress = normalizedAddress;
        suggestion.description = description;
        suggestion.categoryLabel = categoryLabel;
        suggestion.placeType = placeType;
        suggestion.regionLabel = regionLabel;
        suggestion.countryCode = countryCode;
        suggestion.latitude = latitude;
        suggestion.longitude = longitude;
        suggestion.createdAt = Instant.now();
        suggestion.updatedAt = suggestion.createdAt;
        return suggestion;
    }

    public void approve(UUID placeId, String moderator, String reason) {
        if (status == Status.APPROVED) return;
        if (status != Status.PENDING) throw new IllegalStateException("Only pending suggestions may be approved");
        status = Status.APPROVED;
        canonicalPlaceId = placeId;
        moderationReason = reason;
        reviewedBy = moderator;
        reviewedAt = Instant.now();
        updatedAt = reviewedAt;
    }

    public void reject(String moderator, String reason) {
        if (status == Status.REJECTED) return;
        if (status != Status.PENDING) throw new IllegalStateException("Only pending suggestions may be rejected");
        status = Status.REJECTED;
        moderationReason = reason;
        reviewedBy = moderator;
        reviewedAt = Instant.now();
        updatedAt = reviewedAt;
    }

    public UUID getId() { return id; }
    public String getSubmitterUserId() { return submitterUserId; }
    public String getName() { return name; }
    public String getNormalizedName() { return normalizedName; }
    public String getAddress() { return address; }
    public String getNormalizedAddress() { return normalizedAddress; }
    public String getDescription() { return description; }
    public String getCategoryLabel() { return categoryLabel; }
    public String getPlaceType() { return placeType; }
    public String getRegionLabel() { return regionLabel; }
    public String getCountryCode() { return countryCode; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Status getStatus() { return status; }
    public UUID getCanonicalPlaceId() { return canonicalPlaceId; }
    public String getModerationReason() { return moderationReason; }
    public String getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
