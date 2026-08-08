package com.yeyamo_mobile.api.culture_service.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "culture_audit")
public class CultureAuditEvent {
    @Id
    private UUID id;
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;
    @Column(nullable = false)
    private String action;
    @Column(name = "correlation_id", nullable = false)
    private String correlationId;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected CultureAuditEvent() {
    }

    public static CultureAuditEvent recorded(String aggregateId, String action, String correlationId) {
        CultureAuditEvent event = new CultureAuditEvent();
        event.id = UUID.randomUUID();
        event.aggregateId = aggregateId;
        event.action = action;
        event.correlationId = correlationId;
        event.occurredAt = Instant.now();
        return event;
    }
}
