package com.yeyamo_mobile.api.interaction_service.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringCommentLikeRepository extends JpaRepository<CommentLikeEntity, UUID> {
    Optional<CommentLikeEntity> findByCommentIdAndUserId(UUID commentId, String userId);

    long countByCommentId(UUID commentId);
}
