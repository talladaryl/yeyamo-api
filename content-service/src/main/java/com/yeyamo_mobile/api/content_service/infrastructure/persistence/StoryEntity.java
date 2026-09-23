package com.yeyamo_mobile.api.content_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import com.yeyamo_mobile.shared.geography.GeographicFields;
import com.yeyamo_mobile.api.content_service.domain.model.PostReferenceType;

@Entity
@Table(name = "stories")
public class StoryEntity {
    @Id private UUID id;
    @Column(name = "author_id", nullable = false, length = 100) private String authorId;
    @Column(name = "media_id", nullable = false) private UUID mediaId;
    @Column(columnDefinition = "TEXT") private String caption;
    @Column(name = "duration_seconds", nullable = false) private int durationSeconds;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "deleted_at") private Instant deletedAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 32)
    private PostReferenceType referenceType = PostReferenceType.NONE;
    @Column(name = "reference_id", length = 100)
    private String referenceId;
    @Embedded private GeographicFields geography;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public UUID getMediaId() { return mediaId; }
    public void setMediaId(UUID mediaId) { this.mediaId = mediaId; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public PostReferenceType getReferenceType() { return referenceType; }
    public void setReferenceType(PostReferenceType referenceType) { this.referenceType = referenceType == null ? PostReferenceType.NONE : referenceType; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public GeographicFields getGeography() { return geography; }
    public void setGeography(GeographicFields geography) { this.geography = geography; }
}
