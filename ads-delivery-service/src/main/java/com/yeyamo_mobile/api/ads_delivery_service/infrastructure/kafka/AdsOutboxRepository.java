package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.kafka;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AdsOutboxRepository extends JpaRepository<AdsOutboxEvent, UUID> {
    List<AdsOutboxEvent> findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();
}
