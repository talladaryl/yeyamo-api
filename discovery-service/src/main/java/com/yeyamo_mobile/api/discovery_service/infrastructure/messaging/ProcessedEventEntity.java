package com.yeyamo_mobile.api.discovery_service.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity @Table(name = "discovery_processed_events")
public class ProcessedEventEntity {
    @Id private UUID eventId;
    @Column(nullable = false, length = 120) private String eventType;
    @Column(nullable = false) private Instant processedAt;
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID value) { eventId = value; }
    public String getEventType() { return eventType; }
    public void setEventType(String value) { eventType = value; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant value) { processedAt = value; }
}
