package com.yeyamo_mobile.api.place_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.place_service.models.PlaceSuggestion;

public record PlaceSuggestionResponse(
        UUID id, String submitterUserId, String name, String address, String description, String category,
        String placeType, String region, String countryCode, double latitude, double longitude, String status,
        UUID canonicalPlaceId, String moderationReason, Instant reviewedAt, Instant createdAt, Instant updatedAt
) {
    public static PlaceSuggestionResponse from(PlaceSuggestion suggestion) {
        return new PlaceSuggestionResponse(suggestion.getId(), suggestion.getSubmitterUserId(), suggestion.getName(),
                suggestion.getAddress(), suggestion.getDescription(), suggestion.getCategoryLabel(), suggestion.getPlaceType(),
                suggestion.getRegionLabel(), suggestion.getCountryCode(), suggestion.getLatitude(), suggestion.getLongitude(),
                suggestion.getStatus().name(), suggestion.getCanonicalPlaceId(), suggestion.getModerationReason(),
                suggestion.getReviewedAt(), suggestion.getCreatedAt(), suggestion.getUpdatedAt());
    }
}
