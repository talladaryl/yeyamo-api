package com.yeyamo_mobile.api.event_service.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventOutboxRepository extends JpaRepository<EventOutboxMessage, UUID> {
    List<EventOutboxMessage> findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();
}
