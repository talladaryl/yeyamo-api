package com.yeyamo_mobile.api.catalog_service.infrastructure.messaging;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
@Entity @Table(name="catalog_processed_events")
public class ProcessedEventEntity {
    @Id private UUID eventId;
    @Column(nullable=false,length=100) private String eventType;
    @Column(nullable=false) private Instant processedAt;
    public UUID getEventId(){return eventId;} public void setEventId(UUID v){eventId=v;}
    public String getEventType(){return eventType;} public void setEventType(String v){eventType=v;}
    public Instant getProcessedAt(){return processedAt;} public void setProcessedAt(Instant v){processedAt=v;}
}
