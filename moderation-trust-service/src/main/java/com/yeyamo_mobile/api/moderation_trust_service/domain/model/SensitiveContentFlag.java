package com.yeyamo_mobile.api.moderation_trust_service.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Marks a cultural content item as SACRED or COMMUNITY_RESTRICTED.
 * Flagged content must not be published automatically in the public feed.
 */
public class SensitiveContentFlag {

    public enum FlagType { SACRED_CONTENT, COMMUNITY_RESTRICTED, SENSITIVE, AGE_RESTRICTED }
    public enum FlagStatus { PENDING, APPROVED, REJECTED }

    private String id;
    private TargetType targetType;
    private String targetId;
    private FlagType flagType;
    private String reason;
    private String flaggedBy;
    private String approvedBy;
    private FlagStatus status;
    private Instant createdAt;
    private Instant approvedAt;

    /** Factory used when a moderator proactively flags content. */
    public static SensitiveContentFlag create(
            TargetType targetType, String targetId,
            FlagType flagType, String reason, String flaggedBy) {
        if (targetType == null)                          throw new IllegalArgumentException("targetType required");
        if (targetId == null || targetId.isBlank())      throw new IllegalArgumentException("targetId required");
        if (flagType == null)                            throw new IllegalArgumentException("flagType required");
        if (flaggedBy == null || flaggedBy.isBlank())    throw new IllegalArgumentException("flaggedBy required");

        SensitiveContentFlag f = new SensitiveContentFlag();
        f.id         = UUID.randomUUID().toString();
        f.targetType = targetType;
        f.targetId   = targetId;
        f.flagType   = flagType;
        f.reason     = reason;
        f.flaggedBy  = flaggedBy;
        f.status     = FlagStatus.PENDING;
        f.createdAt  = Instant.now();
        return f;
    }

    /**
     * Approve the flag — content must not appear in the public feed.
     */
    public void approve(String adminId) {
        if (status != FlagStatus.PENDING) throw new IllegalStateException("Flag is not pending");
        this.status     = FlagStatus.APPROVED;
        this.approvedBy = adminId;
        this.approvedAt = Instant.now();
    }

    public void reject(String adminId) {
        if (status != FlagStatus.PENDING) throw new IllegalStateException("Flag is not pending");
        this.status     = FlagStatus.REJECTED;
        this.approvedBy = adminId;
        this.approvedAt = Instant.now();
    }

    /** True when this flag should block public feed publication. */
    public boolean blocksPublicFeed() {
        return status == FlagStatus.APPROVED
            && (flagType == FlagType.SACRED_CONTENT || flagType == FlagType.COMMUNITY_RESTRICTED);
    }

    // Getters/Setters
    public String getId()              { return id; }
    public void setId(String v)        { id = v; }
    public TargetType getTargetType()  { return targetType; }
    public void setTargetType(TargetType v) { targetType = v; }
    public String getTargetId()        { return targetId; }
    public void setTargetId(String v)  { targetId = v; }
    public FlagType getFlagType()      { return flagType; }
    public void setFlagType(FlagType v){ flagType = v; }
    public String getReason()          { return reason; }
    public void setReason(String v)    { reason = v; }
    public String getFlaggedBy()       { return flaggedBy; }
    public void setFlaggedBy(String v) { flaggedBy = v; }
    public String getApprovedBy()      { return approvedBy; }
    public void setApprovedBy(String v){ approvedBy = v; }
    public FlagStatus getStatus()      { return status; }
    public void setStatus(FlagStatus v){ status = v; }
    public Instant getCreatedAt()      { return createdAt; }
    public void setCreatedAt(Instant v){ createdAt = v; }
    public Instant getApprovedAt()     { return approvedAt; }
    public void setApprovedAt(Instant v){ approvedAt = v; }
}
