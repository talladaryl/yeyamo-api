package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.kafka;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ads_outbox_events")
public class AdsOutboxEvent {
    @Id public UUID id;
    public String aggregateId;
    public String eventType;
    @Column(columnDefinition = "TEXT") public String payload;
    public Instant occurredAt;
    public Instant publishedAt;
    public int attempts;
    public String lastError;
}
