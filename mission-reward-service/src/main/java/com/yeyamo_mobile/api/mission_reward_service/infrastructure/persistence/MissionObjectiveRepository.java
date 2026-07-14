package com.yeyamo_mobile.api.mission_reward_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface MissionObjectiveRepository extends JpaRepository<MissionObjectiveEntity,UUID>{List<MissionObjectiveEntity>findByEventTypeOrderByPositionAsc(String eventType);List<MissionObjectiveEntity>findByMissionIdOrderByPositionAsc(UUID missionId);}
