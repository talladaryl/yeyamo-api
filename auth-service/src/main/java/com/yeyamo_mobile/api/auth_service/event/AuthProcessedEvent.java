package com.yeyamo_mobile.api.auth_service.event;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity@Table(name="auth_processed_events")public class AuthProcessedEvent{@Id@Column(name="event_id")private UUID eventId;@Column(name="event_type",nullable=false)private String eventType;@Column(name="processed_at",nullable=false)private Instant processedAt;protected AuthProcessedEvent(){}public AuthProcessedEvent(UUID id,String type){eventId=id;eventType=type;processedAt=Instant.now();}}
