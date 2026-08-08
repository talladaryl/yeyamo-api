package com.yeyamo_mobile.api.moderation_trust_service.infrastructure.persistence;

import com.yeyamo_mobile.api.moderation_trust_service.domain.model.SensitiveContentFlag;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringSensitiveContentFlagRepository extends JpaRepository<SensitiveContentFlagEntity, String> {

    boolean existsByTargetTypeAndTargetIdAndStatus(
            TargetType targetType, String targetId, SensitiveContentFlag.FlagStatus status);

    List<SensitiveContentFlagEntity> findByTargetTypeAndTargetId(
            TargetType targetType, String targetId);
}
