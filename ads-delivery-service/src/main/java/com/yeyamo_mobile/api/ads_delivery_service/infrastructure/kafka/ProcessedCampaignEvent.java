package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.kafka;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ads_processed_events")
public class ProcessedCampaignEvent {
    @Id public UUID eventId;
    public String eventType;
    public Instant processedAt;

    protected ProcessedCampaignEvent() {}

    public ProcessedCampaignEvent(UUID eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = Instant.now();
    }
}
