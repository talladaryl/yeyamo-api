package com.yeyamo_mobile.api.catalog_service.infrastructure.outbox;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface CatalogOutboxEventRepository extends JpaRepository<CatalogOutboxEventEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<CatalogOutboxEventEntity> findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();
}
