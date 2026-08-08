package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.CulturalReviewer;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.ReviewerRole;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.TargetType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringCulturalReviewerRepository extends JpaRepository<CulturalReviewer, String> {

    Optional<CulturalReviewer> findByUserId(String userId);

    List<CulturalReviewer> findByRoleAndStatus(ReviewerRole role, String status);

    /**
     * Reviewers who can handle this content type:
     * - ADMIN (global scope) OR
     * - empty contentTypes set (global scope) OR
     * - contentTypes contains the given type.
     */
    @Query("""
        SELECT r FROM CulturalReviewer r
        WHERE r.status = 'ACTIVE'
          AND (r.role = 'ADMIN'
               OR :contentType MEMBER OF r.contentTypes
               OR r.contentTypes IS EMPTY)
        ORDER BY r.role ASC, r.createdAt ASC
        """)
    List<CulturalReviewer> findEligibleReviewers(
            @Param("contentType") TargetType contentType,
            PageRequest page);
}
