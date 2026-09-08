package com.yeyamo_mobile.api.place_service.dto;

import java.util.List;

/** Stable mobile contract, independent from the OpenRouteService response schema. */
public record DirectionsResponse(
        double distanceMeters,
        double durationSeconds,
        List<Coordinate> geometry
) {
    public record Coordinate(double longitude, double latitude) { }
}
