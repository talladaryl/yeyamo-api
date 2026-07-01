package com.yeyamo_mobile.api.place_service.dto;

import com.yeyamo_mobile.api.place_service.models.District;

public record DistrictResponse(
        Long id,
        Long cityId,
        String name,
        Double latitude,
        Double longitude
) {
    public static DistrictResponse from(District district) {
        return new DistrictResponse(
                district.getId(),
                district.getCity().getId(),
                district.getName(),
                district.getLatitude(),
                district.getLongitude()
        );
    }
}
