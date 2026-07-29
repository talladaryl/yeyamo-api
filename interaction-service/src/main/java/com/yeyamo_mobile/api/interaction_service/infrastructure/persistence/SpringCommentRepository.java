package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.domain.Pageable;import org.springframework.data.jpa.repository.JpaRepository;import com.yeyamo_mobile.api.interaction_service.domain.model.CommentStatus;
public interface SpringCommentRepository extends JpaRepository<CommentEntity,UUID>,org.springframework.data.jpa.repository.JpaSpecificationExecutor<CommentEntity>{List<CommentEntity>findByPostIdAndStatusOrderByCreatedAtAsc(UUID id,CommentStatus s,Pageable p);long countByPostIdAndStatus(UUID id,CommentStatus s);}
