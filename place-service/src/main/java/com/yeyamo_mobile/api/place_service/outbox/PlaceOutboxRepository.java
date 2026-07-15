package com.yeyamo_mobile.api.place_service.outbox;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface PlaceOutboxRepository extends JpaRepository<PlaceOutboxMessage,UUID>{List<PlaceOutboxMessage> findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();}
