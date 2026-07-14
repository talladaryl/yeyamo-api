package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.domain.Pageable;import org.springframework.data.jpa.repository.JpaRepository;import com.yeyamo_mobile.api.interaction_service.domain.model.RelationType;
public interface SpringRelationRepository extends JpaRepository<RelationEntity,UUID>{
 Optional<RelationEntity> findByPostIdAndUserIdAndType(UUID postId,String userId,RelationType type);long countByPostIdAndType(UUID postId,RelationType type);
 List<RelationEntity> findByUserIdAndTypeOrderByCreatedAtDesc(String userId,RelationType type,Pageable page);
}
