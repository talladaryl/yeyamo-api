package com.yeyamo_mobile.api.mission_reward_service.infrastructure.messaging;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity@Table(name="mission_processed_events")public class ProcessedEventEntity{@Id private UUID eventId;@Column(name="event_type",nullable=false,length=140)private String eventType;@Column(name="processed_at",nullable=false)private Instant processedAt;protected ProcessedEventEntity(){}public ProcessedEventEntity(UUID id,String type){eventId=id;eventType=type;processedAt=Instant.now();}}
