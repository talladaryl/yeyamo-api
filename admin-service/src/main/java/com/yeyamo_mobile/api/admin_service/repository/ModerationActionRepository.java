package com.yeyamo_mobile.api.admin_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.admin_service.enums.ModerationTargetType;
import com.yeyamo_mobile.api.admin_service.models.ModerationAction;

public interface ModerationActionRepository extends JpaRepository<ModerationAction, UUID> {
    List<ModerationAction> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(ModerationTargetType targetType, UUID targetId);
}
