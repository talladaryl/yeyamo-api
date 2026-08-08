package com.yeyamo_mobile.api.discovery_service.application;

import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryType;

/**
 * Search criteria for discovery queries.
 * Extended to support Culture & Artisan-specific filters.
 *
 * <p>Culture-specific filters (languageCode, cultureType, materialId, techniqueId,
 * availability, verified) are only applied when the search engine honours them —
 * the PostGIS adapter ignores them and falls back to categoryCode/regionCode;
 * the OpenSearch adapter maps them to term filters on the relevant fields.</p>
 */
public record DiscoverySearch(
        String query,
        DiscoveryType type,
        String categoryCode,
        String regionCode,
        // geo
        Double latitude,
        Double longitude,
        Double radiusKm,
        // pagination
        int page,
        int size,
        boolean trends,
        // -- culture & artisan filters --
        String countryCode,
        String adminLevel1Id,
        String cityId,
        String languageCode,
        String cultureType,
        String materialId,
        String techniqueId,
        Boolean availability,
        Boolean verified
) {
    public DiscoverySearch {
        page = Math.max(0, page);
        size = Math.max(1, Math.min(50, size));
        if ((latitude == null) != (longitude == null))
            throw new IllegalArgumentException("lat and lng are required together");
        if (latitude != null && (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180))
            throw new IllegalArgumentException("Invalid coordinates");
        if (radiusKm != null && (radiusKm <= 0 || radiusKm > 200))
            throw new IllegalArgumentException("radiusKm must be between 0 and 200");
    }

    /** Convenience constructor — backwards-compatible with existing callers (no culture filters). */
    public DiscoverySearch(
            String query,
            DiscoveryType type,
            String categoryCode,
            String regionCode,
            Double latitude,
            Double longitude,
            Double radiusKm,
            int page,
            int size,
            boolean trends) {
        this(query, type, categoryCode, regionCode, latitude, longitude, radiusKm,
             page, size, trends,
             null, null, null, null, null, null, null, null, null);
    }
}
