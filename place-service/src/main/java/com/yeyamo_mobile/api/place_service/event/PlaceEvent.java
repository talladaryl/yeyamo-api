package com.yeyamo_mobile.api.place_service.event;

import java.time.Instant;
import java.util.UUID;

public record PlaceEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        PlaceEventPayload payload
) {
    public static PlaceEvent created(PlaceEventPayload payload) {
        return new PlaceEvent(
                UUID.randomUUID(),
                "place.created",
                1,
                Instant.now(),
                "place-service",
                payload
        );
    }

    public static PlaceEvent updated(PlaceEventPayload payload) {
        return new PlaceEvent(
                UUID.randomUUID(),
                "place.updated",
                1,
                Instant.now(),
                "place-service",
                payload
        );
    }
}
