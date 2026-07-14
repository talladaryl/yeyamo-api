package com.yeyamo_mobile.api.ingestion_service.infrastructure.outbox;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface IngestionOutboxRepository extends JpaRepository<IngestionOutboxEvent,UUID>{List<IngestionOutboxEvent> findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();}
