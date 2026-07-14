package com.yeyamo_mobile.api.partner_service.infrastructure.messaging;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity @Table(name="partner_processed_events")public class PartnerProcessedEvent{@Id@Column(name="event_id")private UUID eventId;@Column(name="event_type",nullable=false)private String eventType;@Column(name="processed_at",nullable=false)private Instant processedAt;protected PartnerProcessedEvent(){}public PartnerProcessedEvent(UUID id,String type){eventId=id;eventType=type;processedAt=Instant.now();}}
