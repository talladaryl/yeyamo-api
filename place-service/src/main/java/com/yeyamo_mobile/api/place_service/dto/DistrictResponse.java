package com.yeyamo_mobile.api.place_service.dto;

import java.util.UUID;

import com.yeyamo_mobile.api.place_service.models.District;

public record DistrictResponse(
        Long id,
        UUID cityId,
        String name,
        Double latitude,
        Double longitude,
        boolean active
) {
    public static DistrictResponse from(District district) {
        return new DistrictResponse(
                district.getId(),
                district.getCity().getId(),
                district.getName(),
                district.getLatitude(),
                district.getLongitude(),
                district.isActive()
        );
    }
}
