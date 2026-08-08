package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.SensitiveContentFlag;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.TargetType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "sensitive_content_flags", indexes = {
    @Index(name = "idx_sensitive_flag_target", columnList = "target_type,target_id"),
    @Index(name = "idx_sensitive_flag_status", columnList = "status")
})
public class SensitiveContentFlagEntity {

    @Id private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false, length = 120)
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_type", nullable = false, length = 40)
    private SensitiveContentFlag.FlagType flagType;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "flagged_by", nullable = false, length = 120)
    private String flaggedBy;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SensitiveContentFlag.FlagStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    public String getId()                                    { return id; }
    public void setId(String v)                              { id = v; }
    public TargetType getTargetType()                        { return targetType; }
    public void setTargetType(TargetType v)                  { targetType = v; }
    public String getTargetId()                              { return targetId; }
    public void setTargetId(String v)                        { targetId = v; }
    public SensitiveContentFlag.FlagType getFlagType()       { return flagType; }
    public void setFlagType(SensitiveContentFlag.FlagType v) { flagType = v; }
    public String getReason()                                { return reason; }
    public void setReason(String v)                          { reason = v; }
    public String getFlaggedBy()                             { return flaggedBy; }
    public void setFlaggedBy(String v)                       { flaggedBy = v; }
    public String getApprovedBy()                            { return approvedBy; }
    public void setApprovedBy(String v)                      { approvedBy = v; }
    public SensitiveContentFlag.FlagStatus getStatus()       { return status; }
    public void setStatus(SensitiveContentFlag.FlagStatus v) { status = v; }
    public Instant getCreatedAt()                            { return createdAt; }
    public void setCreatedAt(Instant v)                      { createdAt = v; }
    public Instant getApprovedAt()                           { return approvedAt; }
    public void setApprovedAt(Instant v)                     { approvedAt = v; }
}
