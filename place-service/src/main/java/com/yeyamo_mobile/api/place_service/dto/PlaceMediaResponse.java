package com.yeyamo_mobile.api.place_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.place_service.enums.MediaType;
import com.yeyamo_mobile.api.place_service.models.PlaceMedia;

public record PlaceMediaResponse(
        Long id,
        String url,
        MediaType type,
        Integer displayOrder,
        Instant createdAt,
        UUID mediaId
) {
    public static PlaceMediaResponse from(PlaceMedia media) {
        return new PlaceMediaResponse(
                media.getId(),
                media.getUrl(),
                media.getType(),
                media.getDisplayOrder(),
                media.getCreatedAt(),
                media.getMediaId()
        );
    }
}
