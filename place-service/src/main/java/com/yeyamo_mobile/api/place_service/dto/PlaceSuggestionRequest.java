package com.yeyamo_mobile.api.place_service.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PlaceSuggestionRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 500) String address,
        @Size(max = 5000) String description,
        @Size(max = 120) String category,
        @Size(max = 120) String placeType,
        @Size(max = 120) String region,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String countryCode,
        UUID administrativeAreaId,
        UUID cityId,
        UUID localityId,
        @Size(max = 8) List<UUID> mediaIds,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
) { }
