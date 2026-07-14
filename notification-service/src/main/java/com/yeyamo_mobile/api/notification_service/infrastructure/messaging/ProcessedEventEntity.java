package com.yeyamo_mobile.api.notification_service.infrastructure.messaging;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity@Table(name="notification_processed_events")public class ProcessedEventEntity{@Id private UUID eventId;@Column(name="event_type",nullable=false,length=120)private String eventType;@Column(name="processed_at",nullable=false)private Instant processedAt;public ProcessedEventEntity(){}public ProcessedEventEntity(UUID id,String type){eventId=id;eventType=type;processedAt=Instant.now();}public UUID getEventId(){return eventId;}}
