package com.yeyamo_mobile.api.ticket_service.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Transactional Outbox Pattern implementation.
 * Ensures events are published to Kafka exactly once with database transaction guarantees.
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_published", columnList = "published"),
    @Index(name = "idx_outbox_created", columnList = "created_at"),
    @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type,aggregate_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @NotNull
    @Size(min = 3, max = 100)
    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;
    
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;
    
    @NotNull
    @Size(min = 3, max = 100)
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @NotNull
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @NotNull
    @Builder.Default
    @Column(nullable = false)
    private Boolean published = false;
    
    @Column(name = "published_at")
    private Instant publishedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * Mark event as published
     */
    public void markAsPublished() {
        this.published = true;
        this.publishedAt = Instant.now();
    }
}
