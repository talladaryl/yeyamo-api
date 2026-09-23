package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "moderation_reportable_targets")
public class ReportedTargetEntity {
    @Id @Column(name = "target_key", length = 180) String targetKey;
    @Column(name = "target_type", nullable = false, length = 40) String targetType;
    @Column(name = "target_id", nullable = false, length = 120) String targetId;
    @Column(name = "owner_id", length = 120) String ownerId;
    @Column(nullable = false) boolean available;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    public ReportedTargetEntity() { }
    public String getOwnerId() { return ownerId; }
    public boolean isAvailable() { return available; }
    public void setTargetKey(String value) { targetKey = value; }
    public void setTargetType(String value) { targetType = value; }
    public void setTargetId(String value) { targetId = value; }
    public void setOwnerId(String value) { ownerId = value; }
    public void setAvailable(boolean value) { available = value; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
}
