package com.yeyamo_mobile.api.partner_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "partner_validation_history")
public class PartnerValidationHistoryEntity {
    @Id private UUID id;
    @Column(name="partner_id",nullable=false) private UUID partnerId;
    @Column(name="actor_id",nullable=false,length=100) private String actorId;
    @Column(nullable=false,length=50) private String decision;
    @Column(length=1000) private String reason;
    @Column(length=1000) private String comment;
    @Column(name="correlation_id",length=100) private String correlationId;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected PartnerValidationHistoryEntity() {}
    public PartnerValidationHistoryEntity(UUID partnerId,String actorId,String decision,String reason,String comment,String correlationId){this.id=UUID.randomUUID();this.partnerId=partnerId;this.actorId=actorId;this.decision=decision;this.reason=reason;this.comment=comment;this.correlationId=correlationId;this.createdAt=Instant.now();}
    public UUID getId(){return id;} public UUID getPartnerId(){return partnerId;} public String getActorId(){return actorId;} public String getDecision(){return decision;} public String getReason(){return reason;} public String getComment(){return comment;} public String getCorrelationId(){return correlationId;} public Instant getCreatedAt(){return createdAt;}
}
