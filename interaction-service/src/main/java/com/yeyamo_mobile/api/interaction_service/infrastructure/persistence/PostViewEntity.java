package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** One persisted, idempotent view per authenticated account and post. */
@Entity
@Table(name = "interaction_post_views")
public class PostViewEntity {
    @EmbeddedId private PostViewId id;
    @Column(name = "viewed_at", nullable = false) private Instant viewedAt;

    public PostViewId getId() { return id; }
    public void setId(PostViewId id) { this.id = id; }
    public Instant getViewedAt() { return viewedAt; }
    public void setViewedAt(Instant viewedAt) { this.viewedAt = viewedAt; }

    @Embeddable
    public static class PostViewId implements Serializable {
        @Column(name = "post_id", nullable = false) private UUID postId;
        @Column(name = "viewer_id", nullable = false, length = 100) private String viewerId;
        public PostViewId() { }
        public PostViewId(UUID postId, String viewerId) { this.postId = postId; this.viewerId = viewerId; }
        public UUID getPostId() { return postId; }
        public String getViewerId() { return viewerId; }
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof PostViewId that)) return false;
            return java.util.Objects.equals(postId, that.postId) && java.util.Objects.equals(viewerId, that.viewerId);
        }
        @Override public int hashCode() { return java.util.Objects.hash(postId, viewerId); }
    }
}
