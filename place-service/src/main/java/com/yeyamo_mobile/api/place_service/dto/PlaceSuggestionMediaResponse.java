package com.yeyamo_mobile.api.place_service.dto;

import java.util.UUID;

import com.yeyamo_mobile.api.place_service.models.PlaceSuggestionMedia;

public record PlaceSuggestionMediaResponse(UUID mediaId, String type, String contentType, String contentUrl,
        String thumbnailUrl, int displayOrder) {
    public static PlaceSuggestionMediaResponse from(PlaceSuggestionMedia media) {
        return new PlaceSuggestionMediaResponse(media.getMediaId(), media.getType(), media.getContentType(),
                media.getContentUrl(), media.getThumbnailUrl(), media.getDisplayOrder());
    }
}
