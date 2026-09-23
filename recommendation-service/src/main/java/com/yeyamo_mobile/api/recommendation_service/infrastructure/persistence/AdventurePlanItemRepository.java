package com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yeyamo_mobile.api.recommendation_service.application.adventure.AdventureAvailabilityStatus;
import com.yeyamo_mobile.api.recommendation_service.application.adventure.AdventureTargetType;

import java.util.UUID;

public interface AdventurePlanItemRepository extends JpaRepository<AdventurePlanItemEntity, UUID> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AdventurePlanItemEntity i set i.availabilityStatus = :status "
            + "where i.targetType = :targetType and i.targetId = :targetId")
    int updateAvailabilityForTarget(@Param("targetType") AdventureTargetType targetType,
            @Param("targetId") String targetId, @Param("status") AdventureAvailabilityStatus status);
}
