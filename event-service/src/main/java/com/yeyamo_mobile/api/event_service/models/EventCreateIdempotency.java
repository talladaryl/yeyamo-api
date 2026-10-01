package com.yeyamo_mobile.api.event_service.models;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "event_create_idempotency", uniqueConstraints = @UniqueConstraint(
        name = "uk_event_create_idempotency_owner_key", columnNames = { "owner_user_id", "idempotency_key" }))
public class EventCreateIdempotency {
    @Id private UUID id;
    @Column(name = "owner_user_id", nullable = false, length = 120) private String ownerUserId;
    @Column(name = "idempotency_key", nullable = false, length = 160) private String idempotencyKey;
    @Column(name = "event_id", nullable = false) private UUID eventId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public static EventCreateIdempotency create(String ownerUserId, String idempotencyKey, UUID eventId) {
        EventCreateIdempotency value = new EventCreateIdempotency();
        value.id = UUID.randomUUID();
        value.ownerUserId = ownerUserId;
        value.idempotencyKey = idempotencyKey;
        value.eventId = eventId;
        value.createdAt = Instant.now();
        return value;
    }
    public UUID getEventId() { return eventId; }
}
