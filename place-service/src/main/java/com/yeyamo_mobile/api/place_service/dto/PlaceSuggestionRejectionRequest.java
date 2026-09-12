package com.yeyamo_mobile.api.place_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaceSuggestionRejectionRequest(@NotBlank @Size(max = 1000) String reason) { }
