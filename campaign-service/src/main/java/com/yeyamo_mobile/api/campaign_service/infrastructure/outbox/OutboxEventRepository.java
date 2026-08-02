package com.yeyamo_mobile.api.campaign_service.infrastructure.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OutboxEventEntity> findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();
}
