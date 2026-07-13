package com.yeyamo_mobile.api.content_service.infrastructure.outbox;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface ContentOutboxRepository extends JpaRepository<ContentOutboxEvent,UUID>{List<ContentOutboxEvent> findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();}
