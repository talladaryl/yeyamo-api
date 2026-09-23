package com.yeyamo_mobile.api.content_service.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface EventSocialDistributionRepository extends JpaRepository<EventSocialDistributionEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distribution from EventSocialDistributionEntity distribution where distribution.eventId = :eventId")
    Optional<EventSocialDistributionEntity> findByEventIdForUpdate(UUID eventId);
}
