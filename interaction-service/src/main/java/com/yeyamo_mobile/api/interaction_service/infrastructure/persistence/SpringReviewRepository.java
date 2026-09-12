package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringReviewRepository extends JpaRepository<ReviewEntity, UUID>,org.springframework.data.jpa.repository.JpaSpecificationExecutor<ReviewEntity> {
    
    /**
     * Find all reviews for a specific place, ordered by creation date descending
     */
    List<ReviewEntity> findByPlaceIdOrderByCreatedAtDesc(UUID placeId, Pageable pageable);
    
    /**
     * Find all reviews by a specific user, ordered by creation date descending
     */
    List<ReviewEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Find a review by user and place (for checking duplicates)
     */
    Optional<ReviewEntity> findByUserIdAndPlaceId(String userId, UUID placeId);
    
    /**
     * Check if a review exists for this user/place combination
     */
    boolean existsByUserIdAndPlaceId(String userId, UUID placeId);

    org.springframework.data.domain.Page<ReviewEntity> findByTargetTypeAndPlaceIdAndStatusOrderByCreatedAtDesc(String targetType, UUID targetId, String status, Pageable pageable);

    @Query("select count(r), avg(r.rating) from ReviewEntity r where r.targetType = :targetType and r.placeId = :targetId and r.status = 'ACTIVE'")
    Object[] aggregateActive(@Param("targetType") String targetType, @Param("targetId") UUID targetId);
}
