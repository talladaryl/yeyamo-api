package com.yeyamo_mobile.api.mission_reward_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface MissionProgressRepository extends JpaRepository<MissionProgressEntity,UUID>{Optional<MissionProgressEntity>findByUserMissionIdAndObjectiveId(UUID userMissionId,UUID objectiveId);List<MissionProgressEntity>findByUserMissionId(UUID userMissionId);}
