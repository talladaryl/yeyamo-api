package com.yeyamo_mobile.api.content_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Author-scoped retry record for one Story create operation. */
@Entity
@Table(name = "story_create_idempotency", uniqueConstraints =
        @UniqueConstraint(name = "uk_story_create_idempotency_author_key", columnNames = { "author_id", "idempotency_key" }))
public class StoryCreateIdempotencyEntity {
    @Id private UUID id;
    @Column(name = "author_id", nullable = false, length = 100) private String authorId;
    @Column(name = "idempotency_key", nullable = false, length = 160) private String idempotencyKey;
    @Column(name = "story_id", nullable = false) private UUID storyId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public static StoryCreateIdempotencyEntity of(String authorId, String idempotencyKey, UUID storyId) {
        StoryCreateIdempotencyEntity value = new StoryCreateIdempotencyEntity();
        value.id = UUID.randomUUID();
        value.authorId = authorId;
        value.idempotencyKey = idempotencyKey;
        value.storyId = storyId;
        value.createdAt = Instant.now();
        return value;
    }

    public UUID getStoryId() { return storyId; }
}
