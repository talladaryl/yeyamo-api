package com.yeyamo_mobile.api.content_service.infrastructure.outbox;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity @Table(name="content_outbox")
public class ContentOutboxEvent{
 @Id private UUID id;@Column(name="aggregate_id",nullable=false,length=100)private String aggregateId;@Column(name="event_type",nullable=false,length=100)private String eventType;
 @Column(nullable=false,columnDefinition="TEXT")private String payload;@Column(name="occurred_at",nullable=false)private Instant occurredAt;@Column(name="published_at")private Instant publishedAt;
 @Column(nullable=false)private int attempts;@Column(name="last_error",length=1000)private String lastError;
 public UUID getId(){return id;}public void setId(UUID v){id=v;}public String getAggregateId(){return aggregateId;}public void setAggregateId(String v){aggregateId=v;}public String getEventType(){return eventType;}public void setEventType(String v){eventType=v;}
 public String getPayload(){return payload;}public void setPayload(String v){payload=v;}public Instant getOccurredAt(){return occurredAt;}public void setOccurredAt(Instant v){occurredAt=v;}public Instant getPublishedAt(){return publishedAt;}public void setPublishedAt(Instant v){publishedAt=v;}
 public int getAttempts(){return attempts;}public void setAttempts(int v){attempts=v;}public String getLastError(){return lastError;}public void setLastError(String v){lastError=v;}
}
