package com.yeyamo_mobile.api.place_service.event;

import java.util.UUID;
import java.time.Instant;

public record PlaceEventPayload(
        UUID placeId,
        UUID partnerId,
        String name,
        String slug,
        String description,
        Double latitude,
        Double longitude,
        String category,
        String regionCode,
        String city,
        String district,
        String address,
        String status,
        Instant updatedAt
) {
}
