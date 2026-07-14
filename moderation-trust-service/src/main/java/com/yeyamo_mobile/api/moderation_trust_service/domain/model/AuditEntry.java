package com.yeyamo_mobile.api.moderation_trust_service.domain.model;
import java.time.Instant;import java.util.UUID;
public record AuditEntry(UUID id,String action,String aggregateType,String aggregateId,String actorId,String correlationId,String details,Instant occurredAt){public static AuditEntry create(String a,String t,String id,String actor,String correlation,String details){return new AuditEntry(UUID.randomUUID(),a,t,id,actor,correlation,details,Instant.now());}}
