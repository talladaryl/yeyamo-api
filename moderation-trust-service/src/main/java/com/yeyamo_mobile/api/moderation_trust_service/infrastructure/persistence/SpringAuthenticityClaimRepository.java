package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.AuthenticityClaim;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.AuthenticityStatus;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.TargetType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringAuthenticityClaimRepository extends JpaRepository<AuthenticityClaim, String> {

    Optional<AuthenticityClaim> findByTargetTypeAndTargetId(TargetType type, String targetId);

    boolean existsByTargetTypeAndTargetId(TargetType type, String targetId);

    List<AuthenticityClaim> findByStatusOrderByCreatedAtDesc(
            AuthenticityStatus status, PageRequest page);

    List<AuthenticityClaim> findByAssignedReviewerIdOrderByCreatedAtAsc(
            String reviewerId);
}
