package com.yeyamo_mobile.api.interaction_service.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.interaction_service.domain.model.Comment;
import com.yeyamo_mobile.api.interaction_service.domain.port.CommentRepository;
import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.CommentLikeEntity;
import com.yeyamo_mobile.api.interaction_service.infrastructure.persistence.SpringCommentLikeRepository;

class CommentLikeServiceTest {

    @Test
    void likeIsIdempotentAndReturnsTheCurrentCount() {
        UUID commentId = UUID.randomUUID();
        CommentRepository comments = mock(CommentRepository.class);
        SpringCommentLikeRepository likes = mock(SpringCommentLikeRepository.class);
        Comment comment = Comment.create(UUID.randomUUID(), null, "author", "Comment");
        CommentLikeEntity existing = new CommentLikeEntity(
                UUID.randomUUID(), commentId, "user-1", Instant.now());
        when(comments.findById(commentId)).thenReturn(Optional.of(comment));
        when(likes.findByCommentIdAndUserId(commentId, "user-1"))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(likes.save(org.mockito.ArgumentMatchers.any())).thenReturn(existing);
        when(likes.countByCommentId(commentId)).thenReturn(1L);

        var result = new CommentLikeService(comments, likes).like(commentId, "user-1");

        assertEquals(1, result.likeCount());
        assertTrue(result.liked());
        verify(likes).save(org.mockito.ArgumentMatchers.any());
    }
}
