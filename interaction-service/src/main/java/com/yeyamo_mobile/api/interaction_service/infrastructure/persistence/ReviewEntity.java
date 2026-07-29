package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "reviews")
public class ReviewEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;
    
    @Column(name = "place_id", nullable = false)
    private UUID placeId;
    
    @Column(nullable = false)
    private short rating; // 1-5
    
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(nullable=false,length=20) private String status="ACTIVE";
    @Column(name="deleted_at") private Instant deletedAt;
    @Column(name="deleted_by",length=100) private String deletedBy;
    @Column(name="deletion_reason",length=1000) private String deletionReason;
    
    // ─── GETTERS & SETTERS ──────────────────────────────────────────────────────
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public UUID getPlaceId() {
        return placeId;
    }
    
    public void setPlaceId(UUID placeId) {
        this.placeId = placeId;
    }
    
    public short getRating() {
        return rating;
    }
    
    public void setRating(short rating) {
        this.rating = rating;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    public String getStatus(){return status;}public void setStatus(String v){status=v;}public Instant getDeletedAt(){return deletedAt;}public void setDeletedAt(Instant v){deletedAt=v;}public String getDeletedBy(){return deletedBy;}public void setDeletedBy(String v){deletedBy=v;}public String getDeletionReason(){return deletionReason;}public void setDeletionReason(String v){deletionReason=v;}
}
