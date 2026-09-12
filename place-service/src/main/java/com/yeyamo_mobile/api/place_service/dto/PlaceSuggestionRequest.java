package com.yeyamo_mobile.api.place_service.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PlaceSuggestionRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 500) String address,
        @Size(max = 5000) String description,
        @Size(max = 120) String category,
        @Size(max = 120) String placeType,
        @Size(max = 120) String region,
        @Pattern(regexp = "[A-Z]{2}") String countryCode,
        @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") double longitude
) { }
