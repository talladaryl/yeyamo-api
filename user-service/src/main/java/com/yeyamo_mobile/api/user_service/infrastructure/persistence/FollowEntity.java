package com.yeyamo_mobile.api.user_service.infrastructure.persistence;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.yeyamo_mobile.api.user_service.domain.model.Follow;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "follows")
public class FollowEntity {
    @EmbeddedId private FollowId id;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected FollowEntity() {}

    public FollowEntity(UUID followerId, UUID followeeId, Instant createdAt) {
        this.id = new FollowId(followerId, followeeId);
        this.createdAt = createdAt;
    }

    public static FollowEntity from(Follow domain) {
        return new FollowEntity(domain.followerId(), domain.followeeId(), domain.createdAt());
    }

    public Follow toDomain() {
        return new Follow(id.followerId, id.followeeId, createdAt);
    }

    public UUID getFollowerId() { return id.followerId; }
    public UUID getFolloweeId() { return id.followeeId; }
    public Instant getCreatedAt() { return createdAt; }

    @Embeddable
    public static class FollowId implements Serializable {
        @Column(name = "follower_id") private UUID followerId;
        @Column(name = "followee_id") private UUID followeeId;

        protected FollowId() {}

        public FollowId(UUID followerId, UUID followeeId) {
            this.followerId = followerId;
            this.followeeId = followeeId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FollowId that)) return false;
            return Objects.equals(followerId, that.followerId) && Objects.equals(followeeId, that.followeeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(followerId, followeeId);
        }
    }
}
