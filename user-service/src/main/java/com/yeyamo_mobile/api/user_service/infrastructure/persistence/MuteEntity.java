package com.yeyamo_mobile.api.user_service.infrastructure.persistence;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A mute hides another profile's authored content without changing follow or block relations. */
@Entity
@Table(name = "mutes")
public class MuteEntity {
    @EmbeddedId private MuteId id;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected MuteEntity() { }
    public MuteEntity(UUID muterId, UUID mutedId) { this.id = new MuteId(muterId, mutedId); this.createdAt = Instant.now(); }
    public UUID getMuterId() { return id.muterId; }
    public UUID getMutedId() { return id.mutedId; }

    @Embeddable
    public static class MuteId implements Serializable {
        @Column(name = "muter_id") private UUID muterId;
        @Column(name = "muted_id") private UUID mutedId;
        protected MuteId() { }
        public MuteId(UUID muterId, UUID mutedId) { this.muterId = muterId; this.mutedId = mutedId; }
        @Override public boolean equals(Object other) { return other instanceof MuteId that && Objects.equals(muterId, that.muterId) && Objects.equals(mutedId, that.mutedId); }
        @Override public int hashCode() { return Objects.hash(muterId, mutedId); }
    }
}
