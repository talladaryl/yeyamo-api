package com.yeyamo_mobile.api.moderation_trust_service.domain.port;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.SensitiveContentFlag;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.TargetType;

import java.util.List;
import java.util.Optional;

public interface SensitiveContentFlagRepository {
    SensitiveContentFlag save(SensitiveContentFlag flag);
    Optional<SensitiveContentFlag> findById(String id);

    /** True if any APPROVED sensitive flag exists for this target. */
    boolean hasApprovedFlag(TargetType targetType, String targetId);

    /** All flags for a target (pending + approved). */
    List<SensitiveContentFlag> findByTarget(TargetType targetType, String targetId);
}
