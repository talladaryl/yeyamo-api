package com.yeyamo_mobile.api.user_service.infrastructure.persistence;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.yeyamo_mobile.api.user_service.domain.model.Block;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "blocks")
public class BlockEntity {
    @EmbeddedId private BlockId id;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected BlockEntity() {}

    public BlockEntity(UUID blockerId, UUID blockedId, Instant createdAt) {
        this.id = new BlockId(blockerId, blockedId);
        this.createdAt = createdAt;
    }

    public static BlockEntity from(Block domain) {
        return new BlockEntity(domain.blockerId(), domain.blockedId(), domain.createdAt());
    }

    public Block toDomain() {
        return new Block(id.blockerId, id.blockedId, createdAt);
    }

    public UUID getBlockerId() { return id.blockerId; }
    public UUID getBlockedId() { return id.blockedId; }
    public Instant getCreatedAt() { return createdAt; }

    @Embeddable
    public static class BlockId implements Serializable {
        @Column(name = "blocker_id") private UUID blockerId;
        @Column(name = "blocked_id") private UUID blockedId;

        protected BlockId() {}

        public BlockId(UUID blockerId, UUID blockedId) {
            this.blockerId = blockerId;
            this.blockedId = blockedId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockId that)) return false;
            return Objects.equals(blockerId, that.blockerId) && Objects.equals(blockedId, that.blockedId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockerId, blockedId);
        }
    }
}
