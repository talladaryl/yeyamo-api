package com.yeyamo_mobile.api.user_service.application;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yeyamo_mobile.api.user_service.application.exception.UserProfileException;
import com.yeyamo_mobile.api.user_service.application.port.OutboxPort;
import com.yeyamo_mobile.api.user_service.domain.model.Block;
import com.yeyamo_mobile.api.user_service.domain.model.Follow;
import com.yeyamo_mobile.api.user_service.domain.model.ProfileStatus;
import com.yeyamo_mobile.api.user_service.domain.model.ProfileVisibility;
import com.yeyamo_mobile.api.user_service.domain.model.UserProfile;
import com.yeyamo_mobile.api.user_service.domain.port.UserProfileRepository;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.BlockEntity;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.FollowEntity;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.SpringDataBlockRepository;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.SpringDataFollowRepository;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.MuteEntity;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.SpringDataMuteRepository;
import com.yeyamo_mobile.api.user_service.interfaces.rest.dto.FeedAuthorIdentityResponse;

@Service
public class SocialGraphService {
    private static final Logger log = LoggerFactory.getLogger(SocialGraphService.class);
    
    private final UserProfileRepository profileRepository;
    private final SpringDataFollowRepository followRepository;
    private final SpringDataBlockRepository blockRepository;
    private final SpringDataMuteRepository muteRepository;
    private final OutboxPort outbox;

    public SocialGraphService(
            UserProfileRepository profileRepository,
            SpringDataFollowRepository followRepository,
            SpringDataBlockRepository blockRepository,
            SpringDataMuteRepository muteRepository,
            OutboxPort outbox) {
        this.profileRepository = profileRepository;
        this.followRepository = followRepository;
        this.blockRepository = blockRepository;
        this.muteRepository = muteRepository;
        this.outbox = outbox;
    }

    // ─── FOLLOW OPERATIONS ──────────────────────────────────────────────────────

    @Transactional
    public void follow(String followerAuthId, UUID followeeId, String correlationId) {
        UUID followerId = getProfileId(followerAuthId);
        UserProfile followee = requireProfile(followeeId);
        if (followerId.equals(followeeId)) {
            throw new UserProfileException("CANNOT_FOLLOW_YOURSELF", "Vous ne pouvez pas vous suivre vous-même", HttpStatus.BAD_REQUEST);
        }

        // Vérifier qu'il n'y a pas de block
        if (blockRepository.existsBlockInEitherDirection(followerId, followeeId)) {
            throw new UserProfileException("CANNOT_FOLLOW_BLOCKED_USER", "Impossible de suivre cet utilisateur", HttpStatus.FORBIDDEN);
        }

        // Vérifier que le follow n'existe pas déjà
        if (followRepository.existsByIdFollowerIdAndIdFolloweeId(followerId, followeeId)) {
            return; // Idempotent
        }

        Follow follow = Follow.create(followerId, followeeId);
        followRepository.save(FollowEntity.from(follow));
        log.info("event=FOLLOW_PERSISTED followerProfileId={} followeeProfileId={} correlationId={}", followerId, followeeId, correlationId);

        // Event pour notification
        outbox.append("social.followed", followeeId, followerAuthId, correlationId,
                java.util.Map.of("followerId", followerId.toString(), "profileId", followerId.toString(), "followerAuthUserId", followerAuthId,
                        "followeeId", followeeId.toString(), "followeeAuthUserId", followee.getAuthUserId()));
    }

    @Transactional
    public void unfollow(String followerAuthId, UUID followeeId, String correlationId) {
        UUID followerId = getProfileId(followerAuthId);
        requireProfile(followeeId);

        FollowEntity.FollowId id = new FollowEntity.FollowId(followerId, followeeId);
        if (followRepository.existsById(id)) {
            followRepository.deleteById(id);
            log.info("event=FOLLOW_REMOVED followerProfileId={} followeeProfileId={} correlationId={}", followerId, followeeId, correlationId);
            
            outbox.append("social.unfollowed", followeeId, followerAuthId, correlationId,
                    java.util.Map.of("followerId", followerId.toString(), "followeeId", followeeId.toString()));
        }
    }

    @Transactional
    public void removeFollower(String ownerAuthId, UUID followerId, String correlationId) {
        UUID ownerId = getProfileId(ownerAuthId);
        requireProfile(followerId);
        FollowEntity.FollowId id = new FollowEntity.FollowId(followerId, ownerId);
        if (followRepository.existsById(id)) {
            followRepository.deleteById(id);
            outbox.append("social.follower_removed", followerId, ownerAuthId, correlationId,
                    java.util.Map.of("ownerId", ownerId.toString(), "followerId", followerId.toString()));
        }
    }

    @Transactional(readOnly = true)
    public Page<UserProfile> getFollowing(String authUserId, Pageable pageable) {
        return getFollowing(getProfileId(authUserId), pageable);
    }

    @Transactional(readOnly = true)
    public List<String> getFollowingAuthUserIds(String authUserId) {
        return getFollowing(authUserId, PageRequest.of(0, 1_000))
                .getContent().stream()
                .map(UserProfile::getAuthUserId)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<UserProfile> getFollowing(UUID userId, Pageable pageable) {
        requireProfile(userId);
        Page<FollowEntity> follows = followRepository.findFollowing(userId, pageable);
        
        List<UUID> followeeIds = follows.getContent().stream()
                .map(FollowEntity::getFolloweeId)
                .collect(Collectors.toList());
        
        List<UserProfile> profiles = profileRepository.findByIdIn(followeeIds);
        
        return follows.map(f -> profiles.stream()
                .filter(p -> p.getId().equals(f.getFolloweeId()))
                .findFirst()
                .orElse(null))
                .map(p -> p);
    }

    @Transactional(readOnly = true)
    public Page<UserProfile> getFollowers(String authUserId, Pageable pageable) {
        return getFollowers(getProfileId(authUserId), pageable);
    }

    @Transactional(readOnly = true)
    public Page<UserProfile> getFollowers(UUID userId, Pageable pageable) {
        requireProfile(userId);
        Page<FollowEntity> follows = followRepository.findFollowers(userId, pageable);
        
        List<UUID> followerIds = follows.getContent().stream()
                .map(FollowEntity::getFollowerId)
                .collect(Collectors.toList());
        
        List<UserProfile> profiles = profileRepository.findByIdIn(followerIds);
        
        return follows.map(f -> profiles.stream()
                .filter(p -> p.getId().equals(f.getFollowerId()))
                .findFirst()
                .orElse(null))
                .map(p -> p);
    }

    @Transactional(readOnly = true)
    public long countFollowing(String authUserId) {
        return countFollowing(getProfileId(authUserId));
    }

    @Transactional(readOnly = true)
    public long countFollowing(UUID profileId) {
        requireProfile(profileId);
        return followRepository.countFollowing(profileId);
    }

    @Transactional(readOnly = true)
    public long countFollowers(String authUserId) {
        return countFollowers(getProfileId(authUserId));
    }

    @Transactional(readOnly = true)
    public long countFollowers(UUID profileId) {
        requireProfile(profileId);
        return followRepository.countFollowers(profileId);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(String followerAuthId, UUID followeeId) {
        UUID followerId = getProfileId(followerAuthId);
        requireProfile(followeeId);
        return followRepository.existsByIdFollowerIdAndIdFolloweeId(followerId, followeeId);
    }

    // ─── BLOCK OPERATIONS ───────────────────────────────────────────────────────

    @Transactional
    public void block(String blockerAuthId, UUID blockedId, String correlationId) {
        UUID blockerId = getProfileId(blockerAuthId);
        requireProfile(blockedId);
        if (blockerId.equals(blockedId)) {
            throw new UserProfileException("CANNOT_BLOCK_YOURSELF", "Vous ne pouvez pas vous bloquer vous-même", HttpStatus.BAD_REQUEST);
        }

        // Idempotent
        if (blockRepository.existsByIdBlockerIdAndIdBlockedId(blockerId, blockedId)) {
            return;
        }

        // Supprimer les follows dans les deux sens
        FollowEntity.FollowId followId1 = new FollowEntity.FollowId(blockerId, blockedId);
        FollowEntity.FollowId followId2 = new FollowEntity.FollowId(blockedId, blockerId);
        followRepository.deleteById(followId1);
        followRepository.deleteById(followId2);

        // Créer le block
        Block block = Block.create(blockerId, blockedId);
        blockRepository.save(BlockEntity.from(block));

        outbox.append("social.blocked", blockedId, blockerAuthId, correlationId,
                java.util.Map.of("blockerId", blockerId.toString(), "blockedId", blockedId.toString()));
    }

    @Transactional
    public void unblock(String blockerAuthId, UUID blockedId, String correlationId) {
        UUID blockerId = getProfileId(blockerAuthId);
        requireProfile(blockedId);

        BlockEntity.BlockId id = new BlockEntity.BlockId(blockerId, blockedId);
        if (blockRepository.existsById(id)) {
            blockRepository.deleteById(id);
            
            outbox.append("social.unblocked", blockedId, blockerAuthId, correlationId,
                    java.util.Map.of("blockerId", blockerId.toString(), "blockedId", blockedId.toString()));
        }
    }

    @Transactional(readOnly = true)
    public List<UUID> getBlockedUserIds(String authUserId) {
        UUID userId = getProfileId(authUserId);
        return blockRepository.findBlockedIds(userId);
    }

    @Transactional(readOnly = true)
    public List<UserProfile> getBlockedUsers(String authUserId) {
        return profileRepository.findByIdIn(getBlockedUserIds(authUserId));
    }

    @Transactional
    public void mute(String muterAuthId, UUID mutedId, String correlationId) {
        UUID muterId = getProfileId(muterAuthId);
        UserProfile muted = requireProfile(mutedId);
        if (muterId.equals(mutedId)) {
            throw new UserProfileException("CANNOT_MUTE_YOURSELF", "Vous ne pouvez pas vous mettre en sourdine", HttpStatus.BAD_REQUEST);
        }
        if (muteRepository.existsByIdMuterIdAndIdMutedId(muterId, mutedId)) return;
        muteRepository.save(new MuteEntity(muterId, mutedId));
        outbox.append("social.muted", mutedId, muterAuthId, correlationId,
                java.util.Map.of("muterProfileId", muterId.toString(), "mutedProfileId", mutedId.toString(),
                        "muterAuthUserId", muterAuthId, "mutedAuthUserId", muted.getAuthUserId()));
    }

    @Transactional
    public void unmute(String muterAuthId, UUID mutedId, String correlationId) {
        UUID muterId = getProfileId(muterAuthId);
        UserProfile muted = requireProfile(mutedId);
        MuteEntity.MuteId id = new MuteEntity.MuteId(muterId, mutedId);
        if (!muteRepository.existsById(id)) return;
        muteRepository.deleteById(id);
        outbox.append("social.unmuted", mutedId, muterAuthId, correlationId,
                java.util.Map.of("muterProfileId", muterId.toString(), "mutedProfileId", mutedId.toString(),
                        "muterAuthUserId", muterAuthId, "mutedAuthUserId", muted.getAuthUserId()));
    }

    @Transactional(readOnly = true)
    public List<UserProfile> getMutedUsers(String authUserId) {
        return profileRepository.findByIdIn(muteRepository.findMutedIds(getProfileId(authUserId)));
    }

    /**
     * Resolves content-authors in one query for a logged-in viewer.  It is the
     * canonical bridge between content.authorId (auth subject) and the social
     * profile UUID expected by follow/profile navigation.
     */
    @Transactional(readOnly = true)
    public List<FeedAuthorIdentityResponse> resolveContentAuthorIdentities(
            String viewerAuthUserId, List<String> authUserIds) {
        if (authUserIds == null || authUserIds.isEmpty()) return List.of();
        UUID viewerProfileId = getProfileId(viewerAuthUserId);
        List<String> requestedAuthUserIds = authUserIds.stream()
                        .filter(id -> id != null && !id.isBlank())
                        .distinct()
                        .limit(50)
                        .toList();
        List<UserProfile> foundProfiles = profileRepository.findByAuthUserIdIn(requestedAuthUserIds);
        List<UserProfile> visibleProfiles = foundProfiles.stream()
                // A private profile is visible to its owner. Without this
                // condition the Feed loses the canonical identity of the
                // current viewer and can offer a self-follow action.
                .filter(profile -> profile.isVisibleTo(viewerAuthUserId))
                .toList();
        var foundAuthUserIds = foundProfiles.stream().map(UserProfile::getAuthUserId).collect(Collectors.toSet());
        var visibleAuthUserIds = visibleProfiles.stream().map(UserProfile::getAuthUserId).collect(Collectors.toSet());
        var missingAuthUserIds = requestedAuthUserIds.stream().filter(id -> !foundAuthUserIds.contains(id)).toList();
        var notVisibleAuthUserIds = requestedAuthUserIds.stream().filter(id -> foundAuthUserIds.contains(id) && !visibleAuthUserIds.contains(id)).toList();
        log.info("event=CONTENT_AUTHOR_IDENTITIES_RESOLVED viewerAuthUserId={} requested={} found={} resolved={} missingAuthUserIds={} notVisibleAuthUserIds={}",
                viewerAuthUserId, requestedAuthUserIds.size(), foundProfiles.size(), visibleProfiles.size(),
                missingAuthUserIds, notVisibleAuthUserIds);
        return visibleProfiles.stream()
                .map(profile -> FeedAuthorIdentityResponse.from(profile,
                        followRepository.existsByIdFollowerIdAndIdFolloweeId(viewerProfileId, profile.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedAuthorIdentityResponse> resolveMessagingIdentities(
            String viewerAuthUserId, List<String> authUserIds) {
        // Requiring the viewer profile prevents anonymous enumeration. Only the
        // minimal presentation fields from FeedAuthorIdentityResponse are exposed.
        getProfileId(viewerAuthUserId);
        if (authUserIds == null || authUserIds.isEmpty()) return List.of();
        List<String> requested = authUserIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .limit(50)
                .toList();
        List<UserProfile> profiles = profileRepository.findByAuthUserIdIn(requested);
        log.info("event=MESSAGING_IDENTITIES_RESOLVED viewerAuthUserId={} requested={} resolved={}",
                viewerAuthUserId, requested.size(), profiles.size());
        return profiles.stream()
                .map(profile -> FeedAuthorIdentityResponse.from(profile, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserProfile publicSocialProfile(String viewerAuthUserId, UUID profileId) {
        UserProfile profile = requireProfile(profileId);
        if (!profile.isVisibleTo(viewerAuthUserId)) {
            throw new UserProfileException("PROFILE_NOT_ACCESSIBLE", "Ce profil n'est pas accessible", HttpStatus.FORBIDDEN);
        }
        return profile;
    }

    @Transactional(readOnly = true)
    public UserProfile getSocialSettings(String authUserId) {
        return getProfile(authUserId);
    }

    @Transactional
    public UserProfile updateSocialSettings(
            String authUserId,
            SocialSettingsUpdate update,
            String correlationId) {
        UserProfile profile = getProfile(authUserId);
        profile.updateSocialSettings(
                update.profileVisibility(),
                update.showActivity(),
                update.showFollowers(),
                update.showFollowing(),
                update.notifyNewFollowers(),
                update.notifyFollowRequests(),
                update.notifyMentions(),
                update.notifyActivityUpdates(),
                update.allowSuggestions(),
                update.allowMessagesFromStrangers());
        UserProfile saved = profileRepository.save(profile);
        outbox.append("social.settings_updated", saved.getId(), authUserId, correlationId,
                java.util.Map.of("profileId", saved.getId().toString()));
        return saved;
    }

    // ─── SUGGESTIONS ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserProfile> getSuggestions(String authUserId, int limit) {
        UUID userId = getProfileId(authUserId);
        
        // Récupérer les suggestions de 2ème degré (amis d'amis)
        List<UUID> suggestedIds = followRepository.findSecondDegreeSuggestions(
                userId, 
                PageRequest.of(0, limit * 2)); // On en prend plus pour filtrer ensuite
        
        // Filtrer les profils bloqués et inactifs
        List<UUID> blockedIds = blockRepository.findBlockedIds(userId);
        List<UUID> blockerIds = blockRepository.findBlockerIds(userId);
        
        List<UserProfile> suggestions = profileRepository.findByIdIn(suggestedIds).stream()
                .filter(p -> p.getStatus() == ProfileStatus.ACTIVE)
                .filter(p -> !blockedIds.contains(p.getId()))
                .filter(p -> !blockerIds.contains(p.getId()))
                .limit(limit)
                .collect(Collectors.toList());

        if (suggestions.size() < limit) {
            var excluded = new java.util.HashSet<UUID>();
            excluded.add(userId);
            excluded.addAll(followRepository.findFollowingIds(userId));
            excluded.addAll(blockedIds);
            excluded.addAll(blockerIds);
            excluded.addAll(suggestions.stream().map(UserProfile::getId).toList());
            profileRepository.searchPublic("", PageRequest.of(0, Math.max(limit * 3, 20))).stream()
                    .filter(p -> p.getStatus() == ProfileStatus.ACTIVE)
                    .filter(p -> !excluded.contains(p.getId()))
                    .limit(limit - suggestions.size())
                    .forEach(suggestions::add);
        }
        
        return suggestions;
    }

    // ─── SEARCH ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<UserProfile> searchUsers(String query, Pageable pageable, String requesterAuthId) {
        UUID requesterId = getProfileId(requesterAuthId);
        List<UUID> blockedIds = blockRepository.findBlockedIds(requesterId);
        List<UUID> blockerIds = blockRepository.findBlockerIds(requesterId);
        
        // Rechercher et filtrer les bloqués
        return profileRepository.searchPublic(query, pageable)
                .map(p -> {
                    if (blockedIds.contains(p.getId()) || blockerIds.contains(p.getId())) {
                        return null;
                    }
                    return p;
                })
                .map(p -> p);
    }

    // ─── ACTIVITY ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FollowActivity> getNetworkActivity(String authUserId, int limit) {
        UUID userId = getProfileId(authUserId);
        List<UUID> followingIds = followRepository.findFollowingIds(userId);
        
        if (followingIds.isEmpty()) {
            return List.of();
        }
        
        List<FollowEntity> recentFollows = followRepository.findRecentActivityFromFollowing(
                followingIds, 
                PageRequest.of(0, limit));
        
        // Charger les profils
        List<UUID> allIds = recentFollows.stream()
                .flatMap(f -> List.of(f.getFollowerId(), f.getFolloweeId()).stream())
                .distinct()
                .collect(Collectors.toList());
        
        List<UserProfile> profiles = profileRepository.findByIdIn(allIds);
        
        return recentFollows.stream()
                .map(f -> {
                    UserProfile follower = profiles.stream()
                            .filter(p -> p.getId().equals(f.getFollowerId()))
                            .findFirst().orElse(null);
                    UserProfile followee = profiles.stream()
                            .filter(p -> p.getId().equals(f.getFolloweeId()))
                            .findFirst().orElse(null);
                    
                    if (follower != null && followee != null) {
                        return new FollowActivity(follower, followee, f.getCreatedAt());
                    }
                    return null;
                })
                .filter(a -> a != null)
                .collect(Collectors.toList());
    }

    // ─── HELPERS ────────────────────────────────────────────────────────────────

    private UUID getProfileId(String authUserId) {
        return getProfile(authUserId).getId();
    }

    private UserProfile getProfile(String authUserId) {
        return profileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new UserProfileException(
                        "PROFILE_NOT_FOUND", 
                        "Profil utilisateur introuvable", 
                        HttpStatus.NOT_FOUND));
    }

    private UserProfile requireProfile(UUID profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new UserProfileException(
                        "PROFILE_NOT_FOUND", "Profil utilisateur introuvable", HttpStatus.NOT_FOUND));
    }

    // ─── NESTED CLASSES ─────────────────────────────────────────────────────────

    public record FollowActivity(
            UserProfile follower,
            UserProfile followee,
            java.time.Instant timestamp) {}

    public record SocialSettingsUpdate(
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
    }
}
