package com.yeyamo_mobile.api.mission_reward_service.infrastructure.outbox;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface MissionOutboxRepository extends JpaRepository<MissionOutboxEvent,UUID>{List<MissionOutboxEvent>findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();}
