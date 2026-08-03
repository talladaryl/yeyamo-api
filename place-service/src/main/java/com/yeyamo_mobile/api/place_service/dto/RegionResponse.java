package com.yeyamo_mobile.api.place_service.dto;

import com.yeyamo_mobile.api.place_service.models.Region;

public record RegionResponse(
        Long id,
        String name,
        String slug,
        String code,
        String countryCode,
        String description,
        String coverImage,
        boolean active
) {
    public static RegionResponse from(Region region) {
        return new RegionResponse(
                region.getId(),
                region.getName(),
                region.getSlug(),
                region.getCode(),
                region.getCountryCode(),
                region.getDescription(),
                region.getCoverImage(),
                region.isActive()
        );
    }
}
