package com.yeyamo_mobile.api.moderation_trust_service.domain.port;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.CulturalReviewer;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.ReviewerRole;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.TargetType;

import java.util.List;
import java.util.Optional;

public interface CulturalReviewerRepository {
    CulturalReviewer save(CulturalReviewer reviewer);
    Optional<CulturalReviewer> findById(String id);
    Optional<CulturalReviewer> findByUserId(String userId);
    List<CulturalReviewer> findActiveByRole(ReviewerRole role);

    /**
     * Find active reviewers whose scope covers a given target type.
     * Includes reviewers with empty (global) scope and ADMIN role.
     */
    List<CulturalReviewer> findEligibleReviewers(TargetType contentType, int limit);
}
