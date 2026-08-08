package com.yeyamo_mobile.api.moderation_trust_service.domain.port;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.CopyrightClaim;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.CopyrightClaimStatus;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.TargetType;

import java.util.List;
import java.util.Optional;

public interface CopyrightClaimRepository {
    CopyrightClaim save(CopyrightClaim claim);
    Optional<CopyrightClaim> findById(String id);
    List<CopyrightClaim> findByTargetTypeAndTargetId(TargetType type, String targetId);
    List<CopyrightClaim> findByStatus(CopyrightClaimStatus status, int limit);
    List<CopyrightClaim> findByContentCreatorId(String creatorId);
}
