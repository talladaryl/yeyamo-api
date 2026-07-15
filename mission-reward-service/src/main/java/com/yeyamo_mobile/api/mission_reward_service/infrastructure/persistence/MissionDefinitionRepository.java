package com.yeyamo_mobile.api.mission_reward_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface MissionDefinitionRepository extends JpaRepository<MissionDefinitionEntity,UUID>{boolean existsByCode(String code);List<MissionDefinitionEntity>findAllByOrderByCreatedAtDesc();}
