package com.yeyamo_mobile.api.event_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.event_service.enums.EventStatus;
import com.yeyamo_mobile.api.event_service.models.Event;

public record EventResponse(
        UUID id,
        UUID placeId,
        String ownerUserId,
        String locationName,
        String locationAddress,
        Double locationLatitude,
        Double locationLongitude,
        String visibility,
        boolean allowUninvitedParticipants,
        boolean commentsParticipantsOnly,
        boolean showParticipants,
        boolean sharingEnabled,
        String title,
        String description,
        Instant startAt,
        Instant endAt,
        EventStatus status,
        Integer capacity,
        Integer registeredCount,
        Instant createdAt,
        Instant updatedAt,
        UUID coverMediaId
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getPlaceId(),
                event.getOwnerUserId(),
                event.getLocationName(),
                event.getLocationAddress(),
                event.getLocationLatitude(),
                event.getLocationLongitude(),
                event.getVisibility().name(),
                event.isAllowUninvitedParticipants(),
                event.isCommentsParticipantsOnly(),
                event.isShowParticipants(),
                event.isSharingEnabled(),
                event.getTitle(),
                event.getDescription(),
                event.getStartAt(),
                event.getEndAt(),
                event.getStatus(),
                event.getCapacity(),
                event.getRegisteredCount(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                event.getCoverMediaId()
        );
    }
}
