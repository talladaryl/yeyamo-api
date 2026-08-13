package com.yeyamo_mobile.api.user_service.application;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.user_service.application.exception.UserProfileException;
import com.yeyamo_mobile.api.user_service.application.port.OutboxPort;
import com.yeyamo_mobile.api.user_service.domain.model.Language;
import com.yeyamo_mobile.api.user_service.domain.model.ProfileVisibility;
import com.yeyamo_mobile.api.user_service.domain.model.UserProfile;
import com.yeyamo_mobile.api.user_service.domain.port.UserProfileRepository;

@Service
public class UserProfileService {
    private final UserProfileRepository repository;
    private final OutboxPort outbox;

    public UserProfileService(UserProfileRepository repository, OutboxPort outbox) {
        this.repository = repository;
        this.outbox = outbox;
    }

    @Transactional
    public UserProfile createFromIdentity(String authUserId, String displayName, String correlationId) {
        return repository.findByAuthUserId(authUserId).orElseGet(() -> {
            UserProfile saved = repository.save(UserProfile.create(authUserId, displayName));
            append("profile.created", saved, authUserId, correlationId);
            return saved;
        });
    }

    @Transactional
    public UserProfile createFromIdentityWithLocation(String authUserId, String displayName, 
            String countryCode, UUID cityId, String preferredLanguageCode, String timezone, String correlationId) {
        return repository.findByAuthUserId(authUserId).orElseGet(() -> {
            UserProfile profile = UserProfile.create(authUserId, displayName);
            // Set geographic data from auth-service
            if (countryCode != null && !countryCode.isBlank()) {
                profile.setCountryCode(countryCode);
            }
            if (cityId != null) {
                profile.setCityId(cityId);
            }
            if (preferredLanguageCode != null && !preferredLanguageCode.isBlank()) {
                profile.setPreferredLanguageCode(preferredLanguageCode);
            }
            if (timezone != null && !timezone.isBlank()) {
                profile.setTimezone(timezone);
            }
            UserProfile saved = repository.save(profile);
            append("profile.created", saved, authUserId, correlationId);
            return saved;
        });
    }

    @Transactional(readOnly = true)
    public UserProfile me(String authUserId) { return byAuthUserId(authUserId); }

    @Transactional
    public UserProfile getOrCreate(String authUserId, String displayName, String correlationId) {
        return repository.findByAuthUserId(authUserId)
                .orElseGet(() -> createFromIdentity(authUserId, displayName, correlationId));
    }

    @Transactional(readOnly = true)
    public UserProfile publicProfile(UUID id, String requesterAuthUserId) {
        UserProfile profile = repository.findById(id).orElseThrow(() -> notFound());
        if (!profile.isVisibleTo(requesterAuthUserId)) {
            throw new UserProfileException("PROFILE_NOT_ACCESSIBLE", "Ce profil n'est pas accessible", HttpStatus.FORBIDDEN);
        }
        return profile;
    }

    @Transactional(readOnly = true)
    public Page<UserProfile> search(String query, Pageable pageable) { return repository.searchPublic(query, pageable); }

    @Transactional
    public UserProfile update(String authUserId, String displayName, String avatarUrl, String bio, Language language,
            ProfileVisibility visibility, String correlationId) {
        UserProfile profile = byAuthUserId(authUserId);
        profile.update(displayName, avatarUrl, bio, language, visibility);
        UserProfile saved = repository.save(profile);
        append("profile.updated", saved, authUserId, correlationId);
        return saved;
    }

    @Transactional
    public UserProfile updatePreferences(String authUserId, boolean notifications, boolean locationSharing,
            Long preferredRegionId, String correlationId) {
        UserProfile profile = byAuthUserId(authUserId);
        profile.updatePreferences(notifications, locationSharing, preferredRegionId);
        UserProfile saved = repository.save(profile);
        append("profile.preferences_updated", saved, authUserId, correlationId);
        return saved;
    }

    @Transactional
    public UserProfile updateLocation(String authUserId, String countryCode, UUID adminLevel1Id, UUID adminLevel2Id,
            UUID cityId, UUID localityId, String timezone, String correlationId) {
        UserProfile profile = byAuthUserId(authUserId);
        profile.updateLocation(countryCode, adminLevel1Id, adminLevel2Id, cityId, localityId, timezone);
        UserProfile saved = repository.save(profile);
        appendLocationEvent("profile.location_updated", saved, authUserId, correlationId);
        return saved;
    }

    @Transactional
    public UserProfile updateLanguagePreferences(String authUserId, String preferredLanguageCode,
            java.util.Set<String> contentLanguages, String correlationId) {
        UserProfile profile = byAuthUserId(authUserId);
        profile.updateLanguagePreferences(preferredLanguageCode, contentLanguages);
        UserProfile saved = repository.save(profile);
        appendLanguageEvent("profile.language_updated", saved, authUserId, correlationId);
        return saved;
    }

    @Transactional
    public UserProfile updateDiscoveryPreferences(String authUserId, java.util.Set<String> contentCountries,
            Integer localRadiusKm, Boolean discoverAfricanContent, String preferredCurrencyCode, String correlationId) {
        UserProfile profile = byAuthUserId(authUserId);
        profile.updateDiscoveryPreferences(contentCountries, localRadiusKm,
                discoverAfricanContent != null ? discoverAfricanContent : profile.isDiscoverAfricanContent(),
                preferredCurrencyCode);
        UserProfile saved = repository.save(profile);
        appendDiscoveryEvent("profile.discovery_preferences_updated", saved, authUserId, correlationId);
        return saved;
    }

    @Transactional
    public void delete(String authUserId, String correlationId) {
        UserProfile profile = byAuthUserId(authUserId);
        profile.delete();
        UserProfile saved = repository.save(profile);
        append("profile.deleted", saved, authUserId, correlationId);
    }

    private UserProfile byAuthUserId(String id) {
        return repository.findByAuthUserId(id).orElseThrow(() -> notFound());
    }

    private UserProfileException notFound() {
        return new UserProfileException("PROFILE_NOT_FOUND", "Profil utilisateur introuvable", HttpStatus.NOT_FOUND);
    }

    private void append(String type, UserProfile p, String actor, String correlationId) {
        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("profileId", p.getId().toString()); payload.put("authUserId", p.getAuthUserId());
        payload.put("displayName", p.getDisplayName()); payload.put("status", p.getStatus().name());
        payload.put("avatarUrl", p.getAvatarUrl());
        payload.put("language", p.getLanguage().name()); payload.put("notificationsEnabled", p.isNotificationsEnabled());
        payload.put("locationSharingEnabled", p.isLocationSharingEnabled()); payload.put("preferredRegionId", p.getPreferredRegionId());
        outbox.append(type, p.getId(), actor, correlationId, payload);
    }

    private void appendLocationEvent(String type, UserProfile p, String actor, String correlationId) {
        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("profileId", p.getId().toString());
        payload.put("authUserId", p.getAuthUserId());
        payload.put("countryCode", p.getCountryCode());
        payload.put("adminLevel1Id", p.getAdminLevel1Id() != null ? p.getAdminLevel1Id().toString() : null);
        payload.put("adminLevel2Id", p.getAdminLevel2Id() != null ? p.getAdminLevel2Id().toString() : null);
        payload.put("cityId", p.getCityId() != null ? p.getCityId().toString() : null);
        payload.put("localityId", p.getLocalityId() != null ? p.getLocalityId().toString() : null);
        payload.put("timezone", p.getTimezone());
        outbox.append(type, p.getId(), actor, correlationId, payload);
    }

    private void appendLanguageEvent(String type, UserProfile p, String actor, String correlationId) {
        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("profileId", p.getId().toString());
        payload.put("authUserId", p.getAuthUserId());
        payload.put("preferredLanguageCode", p.getPreferredLanguageCode());
        payload.put("contentLanguages", p.getContentLanguages());
        outbox.append(type, p.getId(), actor, correlationId, payload);
    }

    private void appendDiscoveryEvent(String type, UserProfile p, String actor, String correlationId) {
        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("profileId", p.getId().toString());
        payload.put("authUserId", p.getAuthUserId());
        payload.put("contentCountries", p.getContentCountries());
        payload.put("localRadiusKm", p.getLocalRadiusKm());
        payload.put("discoverAfricanContent", p.isDiscoverAfricanContent());
        payload.put("preferredCurrencyCode", p.getPreferredCurrencyCode());
        outbox.append(type, p.getId(), actor, correlationId, payload);
    }
}
