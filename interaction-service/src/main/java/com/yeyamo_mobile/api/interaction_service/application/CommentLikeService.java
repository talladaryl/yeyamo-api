package com.yeyamo_mobile.api.interaction_service.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.interaction_service.domain.model.CommentStatus;
import com.yeyamo_mobile.api.interaction_service.domain.port.CommentRepository;
import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.CommentLikeEntity;
import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.SpringCommentLikeRepository;

@Service
public class CommentLikeService {
    private final CommentRepository comments;
    private final SpringCommentLikeRepository likes;

    public CommentLikeService(CommentRepository comments, SpringCommentLikeRepository likes) {
        this.comments = comments;
        this.likes = likes;
    }

    @Transactional
    public CommentLikeStatus like(UUID commentId, String userId) {
        requireActiveComment(commentId);
        likes.findByCommentIdAndUserId(commentId, userId)
                .orElseGet(() -> likes.save(new CommentLikeEntity(
                        UUID.randomUUID(), commentId, userId, Instant.now())));
        return status(commentId, userId);
    }

    @Transactional
    public CommentLikeStatus unlike(UUID commentId, String userId) {
        requireActiveComment(commentId);
        likes.findByCommentIdAndUserId(commentId, userId).ifPresent(likes::delete);
        return status(commentId, userId);
    }

    @Transactional(readOnly = true)
    public CommentLikeStatus status(UUID commentId, String userId) {
        requireActiveComment(commentId);
        return new CommentLikeStatus(
                commentId,
                likes.countByCommentId(commentId),
                userId != null && likes.findByCommentIdAndUserId(commentId, userId).isPresent()
        );
    }

    private void requireActiveComment(UUID commentId) {
        comments.findById(commentId)
                .filter(comment -> comment.getStatus() == CommentStatus.ACTIVE)
                .orElseThrow(() -> new InteractionException(
                        "COMMENT_NOT_FOUND", "Comment not found"));
    }

    public record CommentLikeStatus(UUID commentId, long likeCount, boolean liked) {
    }
}
