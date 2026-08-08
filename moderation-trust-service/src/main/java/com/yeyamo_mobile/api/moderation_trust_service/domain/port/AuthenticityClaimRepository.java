package com.yeyamo_mobile.api.moderation_trust_service.domain.port;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.AuthenticityClaim;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.AuthenticityStatus;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.TargetType;

import java.util.List;
import java.util.Optional;

public interface AuthenticityClaimRepository {
    AuthenticityClaim save(AuthenticityClaim claim);
    Optional<AuthenticityClaim> findById(String id);
    Optional<AuthenticityClaim> findByTargetTypeAndTargetId(TargetType type, String targetId);
    List<AuthenticityClaim> findByStatus(AuthenticityStatus status, int limit);
    List<AuthenticityClaim> findByAssignedReviewerId(String reviewerId);
    boolean existsByTargetTypeAndTargetId(TargetType type, String targetId);
}
