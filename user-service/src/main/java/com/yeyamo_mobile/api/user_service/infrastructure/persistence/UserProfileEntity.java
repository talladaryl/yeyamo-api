package com.yeyamo_mobile.api.user_service.infrastructure.persistence;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.yeyamo_mobile.api.user_service.domain.model.Language;
import com.yeyamo_mobile.api.user_service.domain.model.ProfileStatus;
import com.yeyamo_mobile.api.user_service.domain.model.ProfileVisibility;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
    @Column(name = "show_activity", nullable = false) private boolean showActivity;
    @Column(name = "show_followers", nullable = false) private boolean showFollowers;
    @Column(name = "show_following", nullable = false) private boolean showFollowing;
    @Column(name = "notify_new_followers", nullable = false) private boolean notifyNewFollowers;
    @Column(name = "notify_follow_requests", nullable = false) private boolean notifyFollowRequests;
    @Column(name = "notify_mentions", nullable = false) private boolean notifyMentions;
    @Column(name = "notify_activity_updates", nullable = false) private boolean notifyActivityUpdates;
    @Column(name = "allow_suggestions", nullable = false) private boolean allowSuggestions;
    @Column(name = "allow_messages_from_strangers", nullable = false) private boolean allowMessagesFromStrangers;
    
    // Multi-country geographic fields
    @Column(name = "country_code", length = 2) private String countryCode;
    @Column(name = "admin_level_1_id") private UUID adminLevel1Id;
    @Column(name = "admin_level_2_id") private UUID adminLevel2Id;
    @Column(name = "city_id") private UUID cityId;
    @Column(name = "locality_id") private UUID localityId;
    @Column(name = "preferred_language_code", length = 10) private String preferredLanguageCode;
    @Column(length = 50) private String timezone;
    @Column(name = "preferred_currency_code", length = 3) private String preferredCurrencyCode;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_content_countries", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "country_code", length = 2)
    private Set<String> contentCountries = new HashSet<>();
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_content_languages", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "language_code", length = 10)
    private Set<String> contentLanguages = new HashSet<>();
    
    @Column(name = "local_radius_km") private Integer localRadiusKm;
    @Column(name = "discover_african_content", nullable = false) private boolean discoverAfricanContent;
    
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
    public boolean isShowActivity() { return showActivity; }
    public void setShowActivity(boolean value) { this.showActivity = value; }
    public boolean isShowFollowers() { return showFollowers; }
    public void setShowFollowers(boolean value) { this.showFollowers = value; }
    public boolean isShowFollowing() { return showFollowing; }
    public void setShowFollowing(boolean value) { this.showFollowing = value; }
    public boolean isNotifyNewFollowers() { return notifyNewFollowers; }
    public void setNotifyNewFollowers(boolean value) { this.notifyNewFollowers = value; }
    public boolean isNotifyFollowRequests() { return notifyFollowRequests; }
    public void setNotifyFollowRequests(boolean value) { this.notifyFollowRequests = value; }
    public boolean isNotifyMentions() { return notifyMentions; }
    public void setNotifyMentions(boolean value) { this.notifyMentions = value; }
    public boolean isNotifyActivityUpdates() { return notifyActivityUpdates; }
    public void setNotifyActivityUpdates(boolean value) { this.notifyActivityUpdates = value; }
    public boolean isAllowSuggestions() { return allowSuggestions; }
    public void setAllowSuggestions(boolean value) { this.allowSuggestions = value; }
    public boolean isAllowMessagesFromStrangers() { return allowMessagesFromStrangers; }
    public void setAllowMessagesFromStrangers(boolean value) { this.allowMessagesFromStrangers = value; }
    
    // Geographic getters/setters
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public UUID getAdminLevel1Id() { return adminLevel1Id; }
    public void setAdminLevel1Id(UUID adminLevel1Id) { this.adminLevel1Id = adminLevel1Id; }
    public UUID getAdminLevel2Id() { return adminLevel2Id; }
    public void setAdminLevel2Id(UUID adminLevel2Id) { this.adminLevel2Id = adminLevel2Id; }
    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }
    public UUID getLocalityId() { return localityId; }
    public void setLocalityId(UUID localityId) { this.localityId = localityId; }
    public String getPreferredLanguageCode() { return preferredLanguageCode; }
    public void setPreferredLanguageCode(String preferredLanguageCode) { 
        this.preferredLanguageCode = preferredLanguageCode; 
    }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getPreferredCurrencyCode() { return preferredCurrencyCode; }
    public void setPreferredCurrencyCode(String preferredCurrencyCode) { 
        this.preferredCurrencyCode = preferredCurrencyCode; 
    }
    public Set<String> getContentCountries() { return contentCountries; }
    public void setContentCountries(Set<String> contentCountries) { 
        this.contentCountries = contentCountries; 
    }
    public Set<String> getContentLanguages() { return contentLanguages; }
    public void setContentLanguages(Set<String> contentLanguages) { 
        this.contentLanguages = contentLanguages; 
    }
    public Integer getLocalRadiusKm() { return localRadiusKm; }
    public void setLocalRadiusKm(Integer localRadiusKm) { this.localRadiusKm = localRadiusKm; }
    public boolean isDiscoverAfricanContent() { return discoverAfricanContent; }
    public void setDiscoverAfricanContent(boolean discoverAfricanContent) { 
        this.discoverAfricanContent = discoverAfricanContent; 
    }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { this.updatedAt = value; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant value) { this.deletedAt = value; }
    public long getVersion() { return version; }
    public void setVersion(long value) { this.version = value; }
}
