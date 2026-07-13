package com.yeyamo_mobile.api.media_service.infrastructure.outbox;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface MediaOutboxRepository extends JpaRepository<MediaOutboxEvent,UUID>{List<MediaOutboxEvent> findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();}
