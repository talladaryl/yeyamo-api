package com.yeyamo_mobile.api.analytics_service.business;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analytics_inbox_processed")
public class AnalyticsInbox {
    @Id public UUID eventId;
    public String eventType;
    public Instant occurredAt;
    public Instant processedAt;

    protected AnalyticsInbox() {}

    public AnalyticsInbox(UUID eventId, String eventType, Instant occurredAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.processedAt = Instant.now();
    }
}
