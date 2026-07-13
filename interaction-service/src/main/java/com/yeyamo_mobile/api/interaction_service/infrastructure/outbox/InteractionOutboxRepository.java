package com.yeyamo_mobile.api.interaction_service.infrastructure.outbox;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface InteractionOutboxRepository extends JpaRepository<InteractionOutboxEvent,UUID>{List<InteractionOutboxEvent>findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();}
