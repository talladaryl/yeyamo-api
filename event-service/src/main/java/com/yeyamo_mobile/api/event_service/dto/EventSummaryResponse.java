package com.yeyamo_mobile.api.event_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.event_service.enums.EventStatus;
import com.yeyamo_mobile.api.event_service.models.Event;

public record EventSummaryResponse(
        UUID id,
        UUID placeId,
        String locationName,
        String visibility,
        String title,
        Instant startAt,
        Instant endAt,
        EventStatus status,
        Integer capacity,
        Integer registeredCount
) {
    public static EventSummaryResponse from(Event event) {
        return new EventSummaryResponse(
                event.getId(),
                event.getPlaceId(),
                event.getLocationName(),
                event.getVisibility().name(),
                event.getTitle(),
                event.getStartAt(),
                event.getEndAt(),
                event.getStatus(),
                event.getCapacity(),
                event.getRegisteredCount()
        );
    }
}
