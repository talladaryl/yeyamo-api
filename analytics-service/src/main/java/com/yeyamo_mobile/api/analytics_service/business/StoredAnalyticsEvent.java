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
    @Column(length = 2) public String countryCode;
    @Column(length = 10) public String languageCode;
    @Column(length = 120) public String adminLevel1Id;
    @Column(length = 120) public String cityId;
    @Column(length = 2) public String userCountryCode;
    @Column(length = 2) public String contentCountryCode;
    @Column(length = 3) public String currencyCode;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb") public String payload;
    public Instant storedAt;
}
