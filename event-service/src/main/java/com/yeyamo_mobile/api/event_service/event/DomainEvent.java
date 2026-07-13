package com.yeyamo_mobile.api.event_service.event;

import java.time.Instant;
import java.util.UUID;

public record DomainEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        String aggregateType,
        String aggregateId,
        String correlationId,
        String actorId,
        DomainEventPayload payload
) {
    public static DomainEvent of(
            String eventType,
            DomainEventPayload payload,
            String correlationId,
            String actorId
    ) {
        return new DomainEvent(
                UUID.randomUUID(),
                eventType,
                1,
                Instant.now(),
                "event-service",
                "event",
                payload.eventId().toString(),
                correlationId,
                actorId,
                payload
        );
    }
}
