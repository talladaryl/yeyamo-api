package com.yeyamo_mobile.api.place_service.event;

import java.time.Instant;
import java.util.UUID;

public record PlaceEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        String aggregateType,
        String aggregateId,
        String correlationId,
        String actorId,
        PlaceEventPayload payload
) {
    public static PlaceEvent created(PlaceEventPayload payload, String correlationId, String actorId) {
        return new PlaceEvent(
                UUID.randomUUID(),
                "place.created",
                2,
                Instant.now(),
                "place-service",
                "place",
                payload.placeId().toString(),
                correlationId,
                actorId,
                payload
        );
    }

    public static PlaceEvent updated(PlaceEventPayload payload, String correlationId, String actorId) {
        return new PlaceEvent(
                UUID.randomUUID(),
                "place.updated",
                2,
                Instant.now(),
                "place-service",
                "place",
                payload.placeId().toString(),
                correlationId,
                actorId,
                payload
        );
    }
}
