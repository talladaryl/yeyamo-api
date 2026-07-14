package com.yeyamo_mobile.api.user_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.user_service.domain.model.Language;
import com.yeyamo_mobile.api.user_service.domain.model.ProfileStatus;
import com.yeyamo_mobile.api.user_service.domain.model.ProfileVisibility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "user_profiles")
public class UserProfileEntity {
    @Id private UUID id;
    @Column(name = "auth_user_id", nullable = false, unique = true, length = 100) private String authUserId;
    @Column(name = "display_name", nullable = false, length = 100) private String displayName;
    @Column(name = "avatar_url", length = 2048) private String avatarUrl;
    @Column(length = 500) private String bio;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Language language;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ProfileVisibility visibility;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ProfileStatus status;
    @Column(name = "notifications_enabled", nullable = false) private boolean notificationsEnabled;
    @Column(name = "location_sharing_enabled", nullable = false) private boolean locationSharingEnabled;
    @Column(name = "preferred_region_id") private Long preferredRegionId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "deleted_at") private Instant deletedAt;
    @Version private long version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAuthUserId() { return authUserId; }
    public void setAuthUserId(String value) { this.authUserId = value; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { this.displayName = value; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String value) { this.avatarUrl = value; }
    public String getBio() { return bio; }
    public void setBio(String value) { this.bio = value; }
    public Language getLanguage() { return language; }
    public void setLanguage(Language value) { this.language = value; }
    public ProfileVisibility getVisibility() { return visibility; }
    public void setVisibility(ProfileVisibility value) { this.visibility = value; }
    public ProfileStatus getStatus() { return status; }
    public void setStatus(ProfileStatus value) { this.status = value; }
    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean value) { this.notificationsEnabled = value; }
    public boolean isLocationSharingEnabled() { return locationSharingEnabled; }
    public void setLocationSharingEnabled(boolean value) { this.locationSharingEnabled = value; }
    public Long getPreferredRegionId() { return preferredRegionId; }
    public void setPreferredRegionId(Long value) { this.preferredRegionId = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { this.updatedAt = value; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant value) { this.deletedAt = value; }
    public long getVersion() { return version; }
    public void setVersion(long value) { this.version = value; }
}
