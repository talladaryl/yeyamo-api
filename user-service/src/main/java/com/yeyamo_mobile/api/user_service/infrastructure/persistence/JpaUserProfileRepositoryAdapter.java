package com.yeyamo_mobile.api.user_service.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.yeyamo_mobile.api.user_service.domain.model.ProfileStatus;
import com.yeyamo_mobile.api.user_service.domain.model.ProfileVisibility;
import com.yeyamo_mobile.api.user_service.domain.model.UserProfile;
import com.yeyamo_mobile.api.user_service.domain.port.UserProfileRepository;

@Repository
public class JpaUserProfileRepositoryAdapter implements UserProfileRepository {
    private final SpringDataUserProfileRepository repository;

    public JpaUserProfileRepositoryAdapter(SpringDataUserProfileRepository repository) {
        this.repository = repository;
    }

    @Override public UserProfile save(UserProfile profile) { return toDomain(repository.save(toEntity(profile))); }
    @Override public Optional<UserProfile> findById(UUID id) { return repository.findById(id).map(this::toDomain); }
    @Override public Optional<UserProfile> findByAuthUserId(String id) { return repository.findByAuthUserId(id).map(this::toDomain); }
    @Override public boolean existsByAuthUserId(String id) { return repository.existsByAuthUserId(id); }
    @Override public Page<UserProfile> searchPublic(String query, Pageable pageable) {
        return repository.searchPublic(query == null ? "" : query.trim(), ProfileStatus.ACTIVE,
                ProfileVisibility.PUBLIC, pageable).map(this::toDomain);
    }
    @Override public List<UserProfile> findByIdIn(List<UUID> ids) {
        return repository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    private UserProfileEntity toEntity(UserProfile p) {
        UserProfileEntity e = new UserProfileEntity();
        e.setId(p.getId()); e.setAuthUserId(p.getAuthUserId()); e.setDisplayName(p.getDisplayName());
        e.setAvatarUrl(p.getAvatarUrl()); e.setBio(p.getBio()); e.setLanguage(p.getLanguage());
        e.setVisibility(p.getVisibility()); e.setStatus(p.getStatus());
        e.setNotificationsEnabled(p.isNotificationsEnabled());
        e.setLocationSharingEnabled(p.isLocationSharingEnabled()); e.setPreferredRegionId(p.getPreferredRegionId());
        e.setShowActivity(p.isShowActivity()); e.setShowFollowers(p.isShowFollowers());
        e.setShowFollowing(p.isShowFollowing()); e.setNotifyNewFollowers(p.isNotifyNewFollowers());
        e.setNotifyFollowRequests(p.isNotifyFollowRequests()); e.setNotifyMentions(p.isNotifyMentions());
        e.setNotifyActivityUpdates(p.isNotifyActivityUpdates()); e.setAllowSuggestions(p.isAllowSuggestions());
        e.setAllowMessagesFromStrangers(p.isAllowMessagesFromStrangers());
        e.setCountryCode(p.getCountryCode()); e.setAdminLevel1Id(p.getAdminLevel1Id()); e.setAdminLevel2Id(p.getAdminLevel2Id());
        e.setCityId(p.getCityId()); e.setLocalityId(p.getLocalityId()); e.setPreferredLanguageCode(p.getPreferredLanguageCode());
        e.setTimezone(p.getTimezone()); e.setPreferredCurrencyCode(p.getPreferredCurrencyCode());
        e.setContentCountries(new java.util.HashSet<>(p.getContentCountries())); e.setContentLanguages(new java.util.HashSet<>(p.getContentLanguages()));
        e.setLocalRadiusKm(p.getLocalRadiusKm()); e.setDiscoverAfricanContent(p.isDiscoverAfricanContent());
        e.setCreatedAt(p.getCreatedAt()); e.setUpdatedAt(p.getUpdatedAt()); e.setDeletedAt(p.getDeletedAt());
        e.setVersion(p.getVersion()); return e;
    }

    private UserProfile toDomain(UserProfileEntity e) {
        UserProfile p = new UserProfile();
        p.setId(e.getId()); p.setAuthUserId(e.getAuthUserId()); p.setDisplayName(e.getDisplayName());
        p.setAvatarUrl(e.getAvatarUrl()); p.setBio(e.getBio()); p.setLanguage(e.getLanguage());
        p.setVisibility(e.getVisibility()); p.setStatus(e.getStatus());
        p.setNotificationsEnabled(e.isNotificationsEnabled());
        p.setLocationSharingEnabled(e.isLocationSharingEnabled()); p.setPreferredRegionId(e.getPreferredRegionId());
        p.setShowActivity(e.isShowActivity()); p.setShowFollowers(e.isShowFollowers());
        p.setShowFollowing(e.isShowFollowing()); p.setNotifyNewFollowers(e.isNotifyNewFollowers());
        p.setNotifyFollowRequests(e.isNotifyFollowRequests()); p.setNotifyMentions(e.isNotifyMentions());
        p.setNotifyActivityUpdates(e.isNotifyActivityUpdates()); p.setAllowSuggestions(e.isAllowSuggestions());
        p.setAllowMessagesFromStrangers(e.isAllowMessagesFromStrangers());
        p.setCountryCode(e.getCountryCode()); p.setAdminLevel1Id(e.getAdminLevel1Id()); p.setAdminLevel2Id(e.getAdminLevel2Id());
        p.setCityId(e.getCityId()); p.setLocalityId(e.getLocalityId()); p.setPreferredLanguageCode(e.getPreferredLanguageCode());
        p.setTimezone(e.getTimezone()); p.setPreferredCurrencyCode(e.getPreferredCurrencyCode());
        p.setContentCountries(e.getContentCountries() == null ? new java.util.HashSet<>() : new java.util.HashSet<>(e.getContentCountries()));
        p.setContentLanguages(e.getContentLanguages() == null ? new java.util.HashSet<>() : new java.util.HashSet<>(e.getContentLanguages()));
        p.setLocalRadiusKm(e.getLocalRadiusKm()); p.setDiscoverAfricanContent(e.isDiscoverAfricanContent());
        p.setCreatedAt(e.getCreatedAt()); p.setUpdatedAt(e.getUpdatedAt()); p.setDeletedAt(e.getDeletedAt());
        p.setVersion(e.getVersion()); return p;
    }
}
