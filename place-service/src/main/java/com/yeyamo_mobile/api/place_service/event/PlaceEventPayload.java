package com.yeyamo_mobile.api.place_service.event;

import java.util.UUID;

public record PlaceEventPayload(
        UUID placeId,
        UUID partnerId,
        String name,
        Double latitude,
        Double longitude,
        String category
) {
}
