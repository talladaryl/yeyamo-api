package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.SanctionType;
import jakarta.persistence.*;

@Entity
@Table(name = "trust_sanctions")
public class SanctionEntity {
    @Id private UUID id;
    @Column(name="subject_id",nullable=false,length=120) private String subjectId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) private SanctionType type;
    @Column(nullable=false,length=2000) private String reason;
    @Column(name="start_at",nullable=false) private Instant startAt;
    @Column(name="end_at") private Instant endAt;
    @Column(name="actor_id",nullable=false,length=120) private String actorId;
    @Column(name="report_id") private UUID reportId;
    @Column(name="created_at",nullable=false) private Instant createdAt;

    public static SanctionEntity create(String subjectId,SanctionType type,String reason,Instant startAt,Instant endAt,String actorId,UUID reportId){
        if(subjectId==null||subjectId.isBlank()||type==null||reason==null||reason.isBlank())throw new IllegalArgumentException("subjectId, type and reason are required");
        if(type==SanctionType.TEMPORARY_SUSPENSION&&endAt==null)throw new IllegalArgumentException("endAt is required for temporary suspension");
        Instant effectiveStart=startAt==null?Instant.now():startAt;
        if(endAt!=null&&!endAt.isAfter(effectiveStart))throw new IllegalArgumentException("endAt must be after startAt");
        SanctionEntity entity=new SanctionEntity();entity.id=UUID.randomUUID();entity.subjectId=subjectId.trim();entity.type=type;entity.reason=reason.trim();entity.startAt=effectiveStart;entity.endAt=endAt;entity.actorId=actorId;entity.reportId=reportId;entity.createdAt=Instant.now();return entity;
    }
    public UUID getId(){return id;} public String getSubjectId(){return subjectId;} public SanctionType getType(){return type;} public String getReason(){return reason;}
    public Instant getStartAt(){return startAt;} public Instant getEndAt(){return endAt;} public String getActorId(){return actorId;} public UUID getReportId(){return reportId;} public Instant getCreatedAt(){return createdAt;}
}
