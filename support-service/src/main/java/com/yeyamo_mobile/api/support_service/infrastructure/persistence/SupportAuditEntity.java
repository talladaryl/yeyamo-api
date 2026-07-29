package com.yeyamo_mobile.api.support_service.infrastructure.persistence;
import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;
@Entity @Table(name="support_audit_log") public class SupportAuditEntity {
 @Id public UUID id; @Column(name="conversation_id",nullable=false) public UUID conversationId; @Column(name="actor_id",nullable=false) public String actorId;
 @Column(nullable=false) public String action; @Column(nullable=false,columnDefinition="TEXT") public String metadata;
 @Column(name="correlation_id") public String correlationId; @Column(name="created_at",nullable=false) public Instant createdAt;
}
