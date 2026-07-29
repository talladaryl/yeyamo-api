package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.*;
import jakarta.persistence.*;

@Entity
@Table(name="moderation_reports")
public class ModerationReportEntity {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(name="target_type",nullable=false,length=30) private TargetType targetType;
    @Column(name="target_id",nullable=false,length=120) private String targetId;
    @Column(name="target_owner_id",length=120) private String targetOwnerId;
    @Column(name="reporter_id",nullable=false,length=120) private String reporterId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) private ReportReason reason;
    @Column(columnDefinition="TEXT") private String details;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ReportStatus status;
    @Column(nullable=false,length=20) private String priority="NORMAL";
    @Column(name="assigned_to",length=120) private String assignedTo;
    @Column(columnDefinition="TEXT") private String resolution;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Column(name="decided_at") private Instant decidedAt;
    @Version private long version;

    public UUID getId(){return id;} public void setId(UUID value){id=value;}
    public TargetType getTargetType(){return targetType;} public void setTargetType(TargetType value){targetType=value;}
    public String getTargetId(){return targetId;} public void setTargetId(String value){targetId=value;}
    public String getTargetOwnerId(){return targetOwnerId;} public void setTargetOwnerId(String value){targetOwnerId=value;}
    public String getReporterId(){return reporterId;} public void setReporterId(String value){reporterId=value;}
    public ReportReason getReason(){return reason;} public void setReason(ReportReason value){reason=value;}
    public String getDetails(){return details;} public void setDetails(String value){details=value;}
    public ReportStatus getStatus(){return status;} public void setStatus(ReportStatus value){status=value;}
    public String getPriority(){return priority;} public void setPriority(String value){priority=value;}
    public String getAssignedTo(){return assignedTo;} public void setAssignedTo(String value){assignedTo=value;}
    public String getResolution(){return resolution;} public void setResolution(String value){resolution=value;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant value){createdAt=value;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant value){updatedAt=value;}
    public Instant getDecidedAt(){return decidedAt;} public void setDecidedAt(Instant value){decidedAt=value;}
    public long getVersion(){return version;} public void setVersion(long value){version=value;}
}
