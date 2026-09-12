package com.yeyamo_mobile.api.place_service.dto;

import java.util.UUID;

import com.yeyamo_mobile.api.place_service.enums.PlaceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceSuggestionModerationRequest(
        @NotNull Long categoryId,
        @NotNull Long regionId,
        @NotNull UUID cityId,
        Long districtId,
        @Size(max = 255) String slug,
        PlaceStatus placeStatus,
        @Size(max = 1000) String reason
) {
    public PlaceStatus effectivePlaceStatus() {
        return placeStatus == null ? PlaceStatus.PUBLISHED : placeStatus;
    }
}
