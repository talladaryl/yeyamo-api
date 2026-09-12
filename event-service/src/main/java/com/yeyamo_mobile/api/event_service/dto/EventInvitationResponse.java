package com.yeyamo_mobile.api.event_service.dto;

import java.time.Instant;
import java.util.UUID;
import com.yeyamo_mobile.api.event_service.models.EventInvitation;

public record EventInvitationResponse(UUID id, UUID eventId, String userId, String invitedBy, Instant createdAt) {
    public static EventInvitationResponse from(EventInvitation invitation) {
        return new EventInvitationResponse(invitation.getId(), invitation.getEventId(), invitation.getUserId(),
                invitation.getInvitedBy(), invitation.getCreatedAt());
    }
}
