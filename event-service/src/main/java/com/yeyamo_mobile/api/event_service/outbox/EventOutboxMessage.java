package com.yeyamo_mobile.api.event_service.outbox;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "event_outbox")
public class EventOutboxMessage {
    @Id private UUID id;
    @Column(name="aggregate_id",nullable=false,length=80) private String aggregateId;
    @Column(name="event_type",nullable=false,length=100) private String eventType;
    @Column(nullable=false,columnDefinition="TEXT") private String payload;
    @Column(name="occurred_at",nullable=false) private Instant occurredAt;
    @Column(name="published_at") private Instant publishedAt;
    @Column(nullable=false) private int attempts;
    @Column(name="last_error",length=1000) private String lastError;
    public UUID getId(){return id;} public void setId(UUID value){id=value;}
    public String getAggregateId(){return aggregateId;} public void setAggregateId(String value){aggregateId=value;}
    public String getEventType(){return eventType;} public void setEventType(String value){eventType=value;}
    public String getPayload(){return payload;} public void setPayload(String value){payload=value;}
    public Instant getOccurredAt(){return occurredAt;} public void setOccurredAt(Instant value){occurredAt=value;}
    public Instant getPublishedAt(){return publishedAt;} public void setPublishedAt(Instant value){publishedAt=value;}
    public int getAttempts(){return attempts;} public void setAttempts(int value){attempts=value;}
    public String getLastError(){return lastError;} public void setLastError(String value){lastError=value;}
}
