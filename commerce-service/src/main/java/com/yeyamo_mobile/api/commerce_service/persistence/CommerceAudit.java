package com.yeyamo_mobile.api.commerce_service.persistence;
import jakarta.persistence.*;import java.time.*;import java.util.*;
@Entity @Table(name="commerce_audit")public class CommerceAudit{@Id public UUID id;@Column(name="actor_id")public String actorId;public String action;@Column(name="entity_type")public String entityType;@Column(name="entity_id")public String entityId;public String details;@Column(name="occurred_at")public Instant occurredAt;}
