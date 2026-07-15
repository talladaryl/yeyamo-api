package com.yeyamo_mobile.api.mission_reward_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface RewardGrantRepository extends JpaRepository<RewardGrantEntity,UUID>{Optional<RewardGrantEntity>findByUserMissionId(UUID userMissionId);List<RewardGrantEntity>findByUserIdOrderByCreatedAtDesc(String userId);}
