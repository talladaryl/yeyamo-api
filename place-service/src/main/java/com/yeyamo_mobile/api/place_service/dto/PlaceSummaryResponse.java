package com.yeyamo_mobile.api.place_service.dto;

import java.util.UUID;

import com.yeyamo_mobile.api.place_service.enums.PlaceStatus;
import com.yeyamo_mobile.api.place_service.models.Place;

public record PlaceSummaryResponse(
        UUID id,
        String name,
        String slug,
        Double latitude,
        Double longitude,
        String address,
        PlaceStatus status,
        String categoryName,
        Double distanceKm
) {
    public static PlaceSummaryResponse from(Place place) {
        return new PlaceSummaryResponse(
                place.getId(),
                place.getName(),
                place.getSlug(),
                place.getLatitude(),
                place.getLongitude(),
                place.getAddress(),
                place.getStatus(),
                place.getCategory() != null ? place.getCategory().getName() : null,
                null
        );
    }
}
