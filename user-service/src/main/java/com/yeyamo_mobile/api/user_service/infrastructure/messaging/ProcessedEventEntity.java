package com.yeyamo_mobile.api.user_service.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity {
    @Id @Column(name = "event_id") private UUID eventId;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(name = "processed_at", nullable = false) private Instant processedAt;

    protected ProcessedEventEntity() {}
    public ProcessedEventEntity(UUID eventId, String eventType) {
        this.eventId = eventId; this.eventType = eventType; this.processedAt = Instant.now();
    }
    public UUID getEventId() { return eventId; }
}
