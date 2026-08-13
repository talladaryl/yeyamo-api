package com.yeyamo_mobile.api.user_service.domain.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class UserProfile {
    private UUID id;
    private String authUserId;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private Language language;
    private ProfileVisibility visibility;
    private ProfileStatus status;
    private boolean notificationsEnabled;
    private boolean locationSharingEnabled;
    private Long preferredRegionId;
    private boolean showActivity;
    private boolean showFollowers;
    private boolean showFollowing;
    private boolean notifyNewFollowers;
    private boolean notifyFollowRequests;
    private boolean notifyMentions;
    private boolean notifyActivityUpdates;
    private boolean allowSuggestions;
    private boolean allowMessagesFromStrangers;
    
    // Multi-country geographic fields
    private String countryCode;
    private UUID adminLevel1Id;
    private UUID adminLevel2Id;
    private UUID cityId;
    private UUID localityId;
    private String preferredLanguageCode;
    private String timezone;
    private String preferredCurrencyCode;
    private Set<String> contentCountries = new HashSet<>();
    private Set<String> contentLanguages = new HashSet<>();
    private Integer localRadiusKm;
    private boolean discoverAfricanContent;
    
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private long version;

    public static UserProfile create(String authUserId, String displayName) {
        Instant now = Instant.now();
        UserProfile profile = new UserProfile();
        profile.id = UUID.randomUUID();
        profile.authUserId = require(authUserId, "authUserId");
        profile.displayName = normalizeDisplayName(displayName, authUserId);
        profile.language = Language.FRENCH;
        profile.visibility = ProfileVisibility.PUBLIC;
        profile.status = ProfileStatus.ACTIVE;
        profile.notificationsEnabled = true;
        profile.showActivity = true;
        profile.showFollowers = true;
        profile.showFollowing = true;
        profile.notifyNewFollowers = true;
        profile.notifyFollowRequests = true;
        profile.notifyMentions = true;
        profile.notifyActivityUpdates = true;
        profile.allowSuggestions = true;
        profile.allowMessagesFromStrangers = true;
        profile.discoverAfricanContent = true;
        profile.createdAt = now;
        profile.updatedAt = now;
        return profile;
    }

    public void update(String displayName, String avatarUrl, String bio, Language language,
            ProfileVisibility visibility) {
        this.displayName = normalizeDisplayName(displayName, authUserId);
        this.avatarUrl = trimToNull(avatarUrl);
        this.bio = trimToNull(bio);
        this.language = language == null ? this.language : language;
        this.visibility = visibility == null ? this.visibility : visibility;
        this.updatedAt = Instant.now();
    }

    public void updatePreferences(boolean notificationsEnabled, boolean locationSharingEnabled, Long preferredRegionId) {
        this.notificationsEnabled = notificationsEnabled;
        this.locationSharingEnabled = locationSharingEnabled;
        this.preferredRegionId = preferredRegionId;
        this.updatedAt = Instant.now();
    }

    public void updateLocation(String countryCode, UUID adminLevel1Id, UUID adminLevel2Id, 
            UUID cityId, UUID localityId, String timezone) {
        if (countryCode != null) this.countryCode = countryCode;
        this.adminLevel1Id = adminLevel1Id;
        this.adminLevel2Id = adminLevel2Id;
        this.cityId = cityId;
        this.localityId = localityId;
        if (timezone != null) this.timezone = timezone;
        this.updatedAt = Instant.now();
    }

    public void updateLanguagePreferences(String preferredLanguageCode, Set<String> contentLanguages) {
        if (preferredLanguageCode != null) this.preferredLanguageCode = preferredLanguageCode;
        if (contentLanguages != null) this.contentLanguages = new HashSet<>(contentLanguages);
        this.updatedAt = Instant.now();
    }

    public void updateDiscoveryPreferences(Set<String> contentCountries, Integer localRadiusKm, 
            boolean discoverAfricanContent, String preferredCurrencyCode) {
        if (contentCountries != null) this.contentCountries = new HashSet<>(contentCountries);
        this.localRadiusKm = localRadiusKm;
        this.discoverAfricanContent = discoverAfricanContent;
        if (preferredCurrencyCode != null) this.preferredCurrencyCode = preferredCurrencyCode;
        this.updatedAt = Instant.now();
    }

    public void updateSocialSettings(
            ProfileVisibility profileVisibility,
            Boolean showActivity,
            Boolean showFollowers,
            Boolean showFollowing,
            Boolean notifyNewFollowers,
            Boolean notifyFollowRequests,
            Boolean notifyMentions,
            Boolean notifyActivityUpdates,
            Boolean allowSuggestions,
            Boolean allowMessagesFromStrangers) {
        if (profileVisibility != null) this.visibility = profileVisibility;
        if (showActivity != null) this.showActivity = showActivity;
        if (showFollowers != null) this.showFollowers = showFollowers;
        if (showFollowing != null) this.showFollowing = showFollowing;
        if (notifyNewFollowers != null) this.notifyNewFollowers = notifyNewFollowers;
        if (notifyFollowRequests != null) this.notifyFollowRequests = notifyFollowRequests;
        if (notifyMentions != null) this.notifyMentions = notifyMentions;
        if (notifyActivityUpdates != null) this.notifyActivityUpdates = notifyActivityUpdates;
        if (allowSuggestions != null) this.allowSuggestions = allowSuggestions;
        if (allowMessagesFromStrangers != null) this.allowMessagesFromStrangers = allowMessagesFromStrangers;
        this.updatedAt = Instant.now();
    }

    public void delete() {
        this.status = ProfileStatus.DELETED;
        this.deletedAt = Instant.now();
        this.updatedAt = this.deletedAt;
        this.displayName = "Utilisateur supprimé";
        this.avatarUrl = null;
        this.bio = null;
        this.locationSharingEnabled = false;
        this.notificationsEnabled = false;
        this.showActivity = false;
        this.showFollowers = false;
        this.showFollowing = false;
        this.allowSuggestions = false;
        this.allowMessagesFromStrangers = false;
    }

    public boolean isVisibleTo(String requesterAuthUserId) {
        if (status != ProfileStatus.ACTIVE) return false;
        if (authUserId.equals(requesterAuthUserId)) return true;
        return visibility == ProfileVisibility.PUBLIC;
    }

    private static String normalizeDisplayName(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? "Utilisateur " + fallback : normalized;
    }

    private static String require(String value, String name) {
        String normalized = trimToNull(value);
        if (normalized == null) throw new IllegalArgumentException(name + " is required");
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAuthUserId() { return authUserId; }
    public void setAuthUserId(String authUserId) { this.authUserId = authUserId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }
    public ProfileVisibility getVisibility() { return visibility; }
    public void setVisibility(ProfileVisibility visibility) { this.visibility = visibility; }
    public ProfileStatus getStatus() { return status; }
    public void setStatus(ProfileStatus status) { this.status = status; }
    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean value) { this.notificationsEnabled = value; }
    public boolean isLocationSharingEnabled() { return locationSharingEnabled; }
    public void setLocationSharingEnabled(boolean value) { this.locationSharingEnabled = value; }
    public Long getPreferredRegionId() { return preferredRegionId; }
    public void setPreferredRegionId(Long preferredRegionId) { this.preferredRegionId = preferredRegionId; }
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
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
