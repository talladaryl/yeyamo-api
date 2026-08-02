package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "interaction_comment_likes",
        uniqueConstraints = @UniqueConstraint(name = "uk_comment_like_comment_user",
                columnNames = {"comment_id", "user_id"}))
public class CommentLikeEntity {
    @Id
    private UUID id;

    @Column(name = "comment_id", nullable = false)
    private UUID commentId;

    @Column(name = "user_id", nullable = false, length = 120)
    private String userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CommentLikeEntity() {
    }

    public CommentLikeEntity(UUID id, UUID commentId, String userId, Instant createdAt) {
        this.id = id;
        this.commentId = commentId;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommentId() {
        return commentId;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
