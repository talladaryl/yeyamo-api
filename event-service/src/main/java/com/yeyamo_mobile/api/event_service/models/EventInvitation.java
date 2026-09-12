package com.yeyamo_mobile.api.event_service.models;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "event_invitations", uniqueConstraints = @UniqueConstraint(name = "uk_event_invitation", columnNames = {"event_id", "user_id"}))
public class EventInvitation {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "event_id", nullable = false) private UUID eventId;
    @Column(name = "user_id", nullable = false, length = 120) private String userId;
    @Column(name = "invited_by", nullable = false, length = 120) private String invitedBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public static EventInvitation create(UUID eventId, String userId, String invitedBy) {
        EventInvitation invitation = new EventInvitation();
        invitation.eventId = eventId;
        invitation.userId = userId;
        invitation.invitedBy = invitedBy;
        invitation.createdAt = Instant.now();
        return invitation;
    }
    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getUserId() { return userId; }
    public String getInvitedBy() { return invitedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
