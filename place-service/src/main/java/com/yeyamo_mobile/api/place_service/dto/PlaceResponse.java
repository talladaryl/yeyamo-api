package com.yeyamo_mobile.api.place_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.yeyamo_mobile.api.place_service.enums.PlaceStatus;
import com.yeyamo_mobile.api.place_service.models.Place;

public record PlaceResponse(
        UUID id,
        UUID partnerId,
        Long categoryId,
        String categoryName,
        Long regionId,
        String regionName,
        Long cityId,
        String cityName,
        Long districtId,
        String districtName,
        String name,
        String slug,
        String description,
        Double latitude,
        Double longitude,
        String geohash,
        String address,
        String phone,
        String website,
        PlaceStatus status,
        List<PlaceMediaResponse> media,
        List<PlaceScheduleResponse> schedules,
        Instant createdAt,
        Instant updatedAt
) {
    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
                place.getId(),
                place.getPartnerId(),
                place.getCategory() != null ? place.getCategory().getId() : null,
                place.getCategory() != null ? place.getCategory().getName() : null,
                place.getRegion().getId(),
                place.getRegion().getName(),
                place.getCity().getId(),
                place.getCity().getName(),
                place.getDistrict() != null ? place.getDistrict().getId() : null,
                place.getDistrict() != null ? place.getDistrict().getName() : null,
                place.getName(),
                place.getSlug(),
                place.getDescription(),
                place.getLatitude(),
                place.getLongitude(),
                place.getGeohash(),
                place.getAddress(),
                place.getPhone(),
                place.getWebsite(),
                place.getStatus(),
                place.getMedia().stream().map(PlaceMediaResponse::from).toList(),
                place.getSchedules().stream().map(PlaceScheduleResponse::from).toList(),
                place.getCreatedAt(),
                place.getUpdatedAt()
        );
    }
}
