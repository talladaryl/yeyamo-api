package com.yeyamo_mobile.api.mission_reward_service.infrastructure.messaging;
import java.util.UUID;import org.springframework.data.jpa.repository.JpaRepository;
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity,UUID>{}
