package com.yeyamo_mobile.api.recommendation_service.domain;

import java.util.List;

/**
 * Runtime context supplied per recommendation request.
 * Extended to carry Culture & Artisan signals without breaking
 * existing callers via a backwards-compatible constructor.
 */
public record RecommendationContext(
        Double latitude,
        Double longitude,
        /** BCP-47 language codes the user speaks / is learning. */
        List<String> languageCodes,
        /**
         * Named recommendation context requested by the client.
         * Values: culture | artworks | artisan | languages | heritage | daily_learning
         */
        String recommendationContext
) {
    public RecommendationContext {
        if ((latitude == null) != (longitude == null))
            throw new IllegalArgumentException("lat and lng are required together");
        if (latitude != null && (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180))
            throw new IllegalArgumentException("invalid coordinates");
        languageCodes = languageCodes == null ? List.of() : List.copyOf(languageCodes);
    }

    /** Backwards-compatible constructor — used by existing controller code. */
    public RecommendationContext(Double latitude, Double longitude) {
        this(latitude, longitude, List.of(), null);
    }
}
