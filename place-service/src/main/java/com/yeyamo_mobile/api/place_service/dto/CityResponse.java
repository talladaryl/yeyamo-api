package com.yeyamo_mobile.api.place_service.dto;

import java.util.UUID;

import com.yeyamo_mobile.api.place_service.models.City;

public record CityResponse(
        UUID id,
        Long regionId,
        String name,
        String slug,
        Double latitude,
        Double longitude,
        boolean active
) {
    public static CityResponse from(City city) {
        return new CityResponse(
                city.getId(),
                city.getRegion().getId(),
                city.getName(),
                city.getSlug(),
                city.getLatitude(),
                city.getLongitude(),
                city.isActive()
        );
    }
}
