package com.yeyamo_mobile.api.catalog_service.infrastructure.outbox;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CatalogOutboxRepository extends JpaRepository<CatalogOutboxEvent, UUID> {
    List<CatalogOutboxEvent> findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();
}
