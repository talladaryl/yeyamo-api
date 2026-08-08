package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.CopyrightClaim;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.CopyrightClaimStatus;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.TargetType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringCopyrightClaimRepository extends JpaRepository<CopyrightClaim, String> {
    List<CopyrightClaim> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            TargetType type, String targetId);

    List<CopyrightClaim> findByStatusOrderByCreatedAtDesc(
            CopyrightClaimStatus status, PageRequest page);

    List<CopyrightClaim> findByContentCreatorIdOrderByCreatedAtDesc(String creatorId);
}
