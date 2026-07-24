package com.yeyamo_mobile.api.catalog_service.infrastructure.persistence;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "collection_places")
@IdClass(CollectionPlaceEntity.CollectionPlaceId.class)
public class CollectionPlaceEntity {
    @Id @Column(name = "collection_id") private UUID collectionId;
    @Id @Column(name = "asset_id") private UUID assetId;
    @Column(name = "added_at", nullable = false, updatable = false) private Instant addedAt;
    @Column(name = "is_priority", nullable = false) private boolean priority;
    @Column(length = 1000) private String note;

    public UUID getCollectionId() { return collectionId; }
    public void setCollectionId(UUID collectionId) { this.collectionId = collectionId; }
    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }
    public boolean isPriority() { return priority; }
    public void setPriority(boolean priority) { this.priority = priority; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public static class CollectionPlaceId implements Serializable {
        private UUID collectionId;
        private UUID assetId;

        public CollectionPlaceId() {}
        public CollectionPlaceId(UUID collectionId, UUID assetId) {
            this.collectionId = collectionId;
            this.assetId = assetId;
        }

        public UUID getCollectionId() { return collectionId; }
        public void setCollectionId(UUID collectionId) { this.collectionId = collectionId; }
        public UUID getAssetId() { return assetId; }
        public void setAssetId(UUID assetId) { this.assetId = assetId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CollectionPlaceId that = (CollectionPlaceId) o;
            return Objects.equals(collectionId, that.collectionId) && Objects.equals(assetId, that.assetId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(collectionId, assetId);
        }
    }
}
