package com.yeyamo_mobile.api.interaction_service.interfaces.rest;

import java.util.UUID;

import com.yeyamo_mobile.api.interaction_service.application.CommentLikeService.CommentLikeStatus;

public record CommentLikeResponse(UUID commentId, long likeCount, boolean liked) {
    public static CommentLikeResponse from(CommentLikeStatus status) {
        return new CommentLikeResponse(status.commentId(), status.likeCount(), status.liked());
    }
}
