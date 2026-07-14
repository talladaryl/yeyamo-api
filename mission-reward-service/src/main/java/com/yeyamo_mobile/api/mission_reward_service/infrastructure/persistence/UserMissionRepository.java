package com.yeyamo_mobile.api.mission_reward_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import jakarta.persistence.LockModeType;
public interface UserMissionRepository extends JpaRepository<UserMissionEntity,UUID>{
 Optional<UserMissionEntity>findByUserIdAndMissionId(String userId,UUID missionId);
 List<UserMissionEntity>findByUserIdOrderByUpdatedAtDesc(String userId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)@Query("select u from UserMissionEntity u where u.id=:id")Optional<UserMissionEntity>findLocked(@Param("id")UUID id);
}
