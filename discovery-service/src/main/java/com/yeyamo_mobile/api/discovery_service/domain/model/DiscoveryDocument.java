package com.yeyamo_mobile.api.discovery_service.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Canonical search document indexed in PostGIS / OpenSearch.
 * Extended to carry Culture & Artisan metadata.
 *
 * <p>Fields added for culture/artisan are nullable — legacy consumers
 * receive {@code null} for those fields and continue to work unchanged.</p>
 */
public record DiscoveryDocument(
        UUID id,
        String sourceId,
        DiscoveryType type,
        String title,
        String description,
        String categoryCode,
        String regionCode,
        String city,
        Double latitude,
        Double longitude,
        String authorId,
        double trendScore,
        boolean active,
        Instant publishedAt,
        Instant updatedAt,

        // --- Culture & Artisan extensions ---
        /** ISO-3166-1 alpha-2. */
        String countryCode,
        String adminLevel1Id,
        String cityId,
        /** Translated titles keyed by BCP-47 language tag, serialised as JSON. */
        String translatedTitlesJson,
        /** BCP-47 codes, comma-separated. */
        String languageCodes,
        String community,
        /** Comma-separated tag values. */
        String tags,
        /** Comma-separated material identifiers (artwork). */
        String materials,
        /** Comma-separated technique identifiers (artwork). */
        String techniques,
        String artisanId,
        /** PENDING / VERIFIED / REJECTED. */
        String verificationStatus,
        /** AVAILABLE / SOLD / ON_HOLD. */
        String availabilityStatus,
        BigDecimal priceMin,
        BigDecimal priceMax,
        /** Popularity signal – view count, interactions etc. Higher = more popular. */
        double popularitySignal
) {
    public DiscoveryDocument {
        if (id == null)                                   throw new IllegalArgumentException("id is required");
        if (sourceId == null || sourceId.isBlank())       throw new IllegalArgumentException("sourceId is required");
        if (type == null)                                 throw new IllegalArgumentException("type is required");
        if (title == null || title.isBlank())             throw new IllegalArgumentException("title is required");
        if ((latitude == null) != (longitude == null))    throw new IllegalArgumentException("latitude and longitude must be provided together");
        if (latitude != null && (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180))
            throw new IllegalArgumentException("Invalid coordinates");
        popularitySignal = Math.max(0, popularitySignal);
    }

    /**
     * Backwards-compatible constructor — no culture/artisan metadata.
     * Used by existing event consumers that were written before the extension.
     */
    public DiscoveryDocument(
            UUID id, String sourceId, DiscoveryType type,
            String title, String description,
            String categoryCode, String regionCode, String city,
            Double latitude, Double longitude, String authorId,
            double trendScore, boolean active,
            Instant publishedAt, Instant updatedAt) {
        this(id, sourceId, type, title, description,
             categoryCode, regionCode, city, latitude, longitude, authorId,
             trendScore, active, publishedAt, updatedAt,
             null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0);
    }
}
