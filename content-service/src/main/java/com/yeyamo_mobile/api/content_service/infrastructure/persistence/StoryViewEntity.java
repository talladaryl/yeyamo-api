package com.yeyamo_mobile.api.content_service.infrastructure.persistence;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "story_views")
@IdClass(StoryViewEntity.StoryViewId.class)
public class StoryViewEntity {
    @Id @Column(name = "story_id") private UUID storyId;
    @Id @Column(name = "viewer_id", length = 100) private String viewerId;
    @Column(name = "viewed_at", nullable = false) private Instant viewedAt;

    public UUID getStoryId() { return storyId; }
    public void setStoryId(UUID storyId) { this.storyId = storyId; }
    public String getViewerId() { return viewerId; }
    public void setViewerId(String viewerId) { this.viewerId = viewerId; }
    public Instant getViewedAt() { return viewedAt; }
    public void setViewedAt(Instant viewedAt) { this.viewedAt = viewedAt; }

    public static class StoryViewId implements Serializable {
        private UUID storyId;
        private String viewerId;

        public StoryViewId() {}
        public StoryViewId(UUID storyId, String viewerId) {
            this.storyId = storyId;
            this.viewerId = viewerId;
        }

        public UUID getStoryId() { return storyId; }
        public void setStoryId(UUID storyId) { this.storyId = storyId; }
        public String getViewerId() { return viewerId; }
        public void setViewerId(String viewerId) { this.viewerId = viewerId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StoryViewId that = (StoryViewId) o;
            return Objects.equals(storyId, that.storyId) && Objects.equals(viewerId, that.viewerId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(storyId, viewerId);
        }
    }
}
