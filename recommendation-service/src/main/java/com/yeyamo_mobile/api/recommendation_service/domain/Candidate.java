package com.yeyamo_mobile.api.recommendation_service.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A compact, event-driven candidate projection shared by recommendations and Adventure Plans. */
public record Candidate(
        String sourceId, String targetId, CandidateKind kind, String title,
        String categoryCode, String regionCode, String countryCode, String languageCode,
        Double latitude, Double longitude, double popularity, boolean active,
        Instant publishedAt, Instant updatedAt,
        BigDecimal price, String currencyCode, UUID imageMediaId, String locationLabel,
        Instant startsAt, Instant endsAt) {
    public Candidate {
        if (sourceId == null || sourceId.isBlank() || targetId == null || targetId.isBlank())
            throw new IllegalArgumentException("candidate identifiers are required");
        if (kind == null || title == null || title.isBlank())
            throw new IllegalArgumentException("candidate kind and title are required");
        if ((latitude == null) != (longitude == null))
            throw new IllegalArgumentException("coordinates must be provided together");
        if (latitude != null && (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180))
            throw new IllegalArgumentException("invalid coordinates");
        if (price != null && price.signum() < 0) throw new IllegalArgumentException("price must be positive");
        countryCode = countryCode == null ? null : countryCode.trim().toUpperCase(java.util.Locale.ROOT);
        languageCode = languageCode == null ? null : languageCode.trim();
        currencyCode = currencyCode == null ? null : currencyCode.trim().toUpperCase(java.util.Locale.ROOT);
        locationLabel = locationLabel == null || locationLabel.isBlank() ? null : locationLabel.trim();
        popularity = Math.max(0, popularity);
    }

    /** Compatibility constructor used by the existing catalog/content projections. */
    public Candidate(String sourceId, String targetId, CandidateKind kind, String title, String categoryCode,
            String regionCode, String countryCode, String languageCode, Double latitude, Double longitude,
            double popularity, boolean active, Instant publishedAt, Instant updatedAt) {
        this(sourceId, targetId, kind, title, categoryCode, regionCode, countryCode, languageCode,
                latitude, longitude, popularity, active, publishedAt, updatedAt,
                null, null, null, null, null, null);
    }

    public Candidate(String sourceId, String targetId, CandidateKind kind, String title, String categoryCode,
            String regionCode, Double latitude, Double longitude, double popularity, boolean active,
            Instant publishedAt, Instant updatedAt) {
        this(sourceId, targetId, kind, title, categoryCode, regionCode, null, null,
                latitude, longitude, popularity, active, publishedAt, updatedAt);
    }
}
