package com.yeyamo_mobile.api.event_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.event_service.enums.EventStatus;
import com.yeyamo_mobile.api.event_service.models.Event;

public record EventResponse(
        UUID id,
        UUID placeId,
        String title,
        String description,
        Instant startAt,
        Instant endAt,
        EventStatus status,
        Integer capacity,
        Integer registeredCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getPlaceId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartAt(),
                event.getEndAt(),
                event.getStatus(),
                event.getCapacity(),
                event.getRegisteredCount(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
