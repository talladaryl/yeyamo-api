package com.yeyamo_mobile.api.analytics_service.business;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analytics_event_store")
public class StoredAnalyticsEvent {
    @Id public UUID eventId;
    public String eventType;
    public Instant occurredAt;
    public String partnerId;
    public String campaignId;
    public String eventEntityId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb") public String payload;
    public Instant storedAt;
}
