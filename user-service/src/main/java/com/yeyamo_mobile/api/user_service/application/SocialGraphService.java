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

import com.yeyamo_mobile.api.user_service.application.exception.UserProfileException;
import com.yeyamo_mobile.api.user_service.application.port.OutboxPort;
import com.yeyamo_mobile.api.user_service.domain.model.Block;
import com.yeyamo_mobile.api.user_service.domain.model.Follow;
import com.yeyamo_mobile.api.user_service.domain.model.ProfileStatus;
import com.yeyamo_mobile.api.user_service.domain.model.UserProfile;
import com.yeyamo_mobile.api.user_service.domain.port.UserProfileRepository;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.BlockEntity;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.FollowEntity;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.SpringDataBlockRepository;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.SpringDataFollowRepository;

@Service
public class SocialGraphService {
    
    private final UserProfileRepository profileRepository;
    private final SpringDataFollowRepository followRepository;
    private final SpringDataBlockRepository blockRepository;
    private final OutboxPort outbox;

    public SocialGraphService(
            UserProfileRepository profileRepository,
            SpringDataFollowRepository followRepository,
            SpringDataBlockRepository blockRepository,
            OutboxPort outbox) {
        this.profileRepository = profileRepository;
        this.followRepository = followRepository;
        this.blockRepository = blockRepository;
        this.outbox = outbox;
    }

    // ─── FOLLOW OPERATIONS ──────────────────────────────────────────────────────

    @Transactional
    public void follow(String followerAuthId, String followeeAuthId, String correlationId) {
        if (followerAuthId.equals(followeeAuthId)) {
            throw new UserProfileException("CANNOT_FOLLOW_YOURSELF", "Vous ne pouvez pas vous suivre vous-même", HttpStatus.BAD_REQUEST);
        }

        UUID followerId = getProfileId(followerAuthId);
        UUID followeeId = getProfileId(followeeAuthId);

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

        // Event pour notification
        outbox.append("social.followed", followeeId, followerAuthId, correlationId,
                java.util.Map.of("followerId", followerId.toString(), "followeeId", followeeId.toString()));
    }

    @Transactional
    public void unfollow(String followerAuthId, String followeeAuthId, String correlationId) {
        UUID followerId = getProfileId(followerAuthId);
        UUID followeeId = getProfileId(followeeAuthId);

        FollowEntity.FollowId id = new FollowEntity.FollowId(followerId, followeeId);
        if (followRepository.existsById(id)) {
            followRepository.deleteById(id);
            
            outbox.append("social.unfollowed", followeeId, followerAuthId, correlationId,
                    java.util.Map.of("followerId", followerId.toString(), "followeeId", followeeId.toString()));
        }
    }

    @Transactional(readOnly = true)
    public Page<UserProfile> getFollowing(String authUserId, Pageable pageable) {
        UUID userId = getProfileId(authUserId);
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
        UUID userId = getProfileId(authUserId);
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
        UUID userId = getProfileId(authUserId);
        return followRepository.countFollowing(userId);
    }

    @Transactional(readOnly = true)
    public long countFollowers(String authUserId) {
        UUID userId = getProfileId(authUserId);
        return followRepository.countFollowers(userId);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(String followerAuthId, String followeeAuthId) {
        UUID followerId = getProfileId(followerAuthId);
        UUID followeeId = getProfileId(followeeAuthId);
        return followRepository.existsByIdFollowerIdAndIdFolloweeId(followerId, followeeId);
    }

    // ─── BLOCK OPERATIONS ───────────────────────────────────────────────────────

    @Transactional
    public void block(String blockerAuthId, String blockedAuthId, String correlationId) {
        if (blockerAuthId.equals(blockedAuthId)) {
            throw new UserProfileException("CANNOT_BLOCK_YOURSELF", "Vous ne pouvez pas vous bloquer vous-même", HttpStatus.BAD_REQUEST);
        }

        UUID blockerId = getProfileId(blockerAuthId);
        UUID blockedId = getProfileId(blockedAuthId);

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
    public void unblock(String blockerAuthId, String blockedAuthId, String correlationId) {
        UUID blockerId = getProfileId(blockerAuthId);
        UUID blockedId = getProfileId(blockedAuthId);

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
        return profileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new UserProfileException(
                        "PROFILE_NOT_FOUND", 
                        "Profil utilisateur introuvable", 
                        HttpStatus.NOT_FOUND))
                .getId();
    }

    // ─── NESTED CLASSES ─────────────────────────────────────────────────────────

    public record FollowActivity(
            UserProfile follower,
            UserProfile followee,
            java.time.Instant timestamp) {}
}
