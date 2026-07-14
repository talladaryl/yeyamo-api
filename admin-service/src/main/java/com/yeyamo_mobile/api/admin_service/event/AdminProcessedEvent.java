package com.yeyamo_mobile.api.admin_service.event;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity@Table(name="admin_processed_events")public class AdminProcessedEvent{@Id@Column(name="event_id")private UUID eventId;@Column(name="event_type",nullable=false)private String eventType;@Column(name="processed_at",nullable=false)private Instant processedAt;protected AdminProcessedEvent(){}public AdminProcessedEvent(UUID id,String type){eventId=id;eventType=type;processedAt=Instant.now();}}
