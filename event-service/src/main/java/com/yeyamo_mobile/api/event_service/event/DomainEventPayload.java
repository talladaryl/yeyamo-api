package com.yeyamo_mobile.api.event_service.event;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.event_service.enums.EventStatus;

public record DomainEventPayload(
        UUID eventId,
        UUID placeId,
        String title,
        Instant startAt,
        EventStatus status
) {
}
