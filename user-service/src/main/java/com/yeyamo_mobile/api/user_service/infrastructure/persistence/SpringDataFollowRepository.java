package com.yeyamo_mobile.api.user_service.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataFollowRepository extends JpaRepository<FollowEntity, FollowEntity.FollowId> {
    
    @Query("SELECT f FROM FollowEntity f WHERE f.id.followerId = :userId ORDER BY f.createdAt DESC")
    Page<FollowEntity> findFollowing(UUID userId, Pageable pageable);

    @Query("SELECT f FROM FollowEntity f WHERE f.id.followeeId = :userId ORDER BY f.createdAt DESC")
    Page<FollowEntity> findFollowers(UUID userId, Pageable pageable);

    @Query("SELECT COUNT(f) FROM FollowEntity f WHERE f.id.followerId = :userId")
    long countFollowing(UUID userId);

    @Query("SELECT COUNT(f) FROM FollowEntity f WHERE f.id.followeeId = :userId")
    long countFollowers(UUID userId);

    boolean existsByIdFollowerIdAndIdFolloweeId(UUID followerId, UUID followeeId);

    @Query("SELECT f.id.followeeId FROM FollowEntity f WHERE f.id.followerId = :userId")
    List<UUID> findFollowingIds(UUID userId);

    @Query("SELECT f.id.followerId FROM FollowEntity f WHERE f.id.followeeId = :userId")
    List<UUID> findFollowerIds(UUID userId);

    // Suggestions: amis d'amis (2ème degré)
    @Query("""
        SELECT DISTINCT f2.id.followeeId 
        FROM FollowEntity f1 
        JOIN FollowEntity f2 ON f1.id.followeeId = f2.id.followerId
        WHERE f1.id.followerId = :userId 
        AND f2.id.followeeId <> :userId
        AND NOT EXISTS (
            SELECT 1 FROM FollowEntity f3 
            WHERE f3.id.followerId = :userId 
            AND f3.id.followeeId = f2.id.followeeId
        )
        """)
    List<UUID> findSecondDegreeSuggestions(UUID userId, Pageable limit);

    // Activité récente: qui parmi mes followings a suivi qui
    @Query("SELECT f FROM FollowEntity f WHERE f.id.followerId IN :followingIds ORDER BY f.createdAt DESC")
    List<FollowEntity> findRecentActivityFromFollowing(List<UUID> followingIds, Pageable limit);
}
