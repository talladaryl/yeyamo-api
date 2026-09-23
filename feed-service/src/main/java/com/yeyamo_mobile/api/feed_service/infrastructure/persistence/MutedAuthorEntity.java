package com.yeyamo_mobile.api.feed_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "feed_muted_authors", uniqueConstraints = @UniqueConstraint(name = "uk_feed_muted_author", columnNames = {"viewer_id", "author_id"}))
public class MutedAuthorEntity {
    @Id UUID id;
    @Column(name = "viewer_id", nullable = false, length = 120) String viewerId;
    @Column(name = "author_id", nullable = false, length = 120) String authorId;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    protected MutedAuthorEntity() { }
    MutedAuthorEntity(String viewerId, String authorId) { this.id = UUID.randomUUID(); this.viewerId = viewerId; this.authorId = authorId; this.createdAt = Instant.now(); }
}
