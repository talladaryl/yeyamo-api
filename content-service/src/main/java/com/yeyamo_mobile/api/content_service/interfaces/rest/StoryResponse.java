package com.yeyamo_mobile.api.content_service.interfaces.rest;

import java.time.Instant;
import java.util.UUID;
import com.yeyamo_mobile.api.content_service.application.StoryService;
import com.yeyamo_mobile.api.content_service.infrastructure.persistence.StoryEntity;

public record StoryResponse(
    UUID id,
    String authorId,
    UUID mediaId,
    String caption,
    int durationSeconds,
    Instant createdAt,
    Instant expiresAt,
    long viewCount,
    boolean viewedByMe
) {
    public static StoryResponse from(StoryEntity story) {
        return new StoryResponse(
            story.getId(),
            story.getAuthorId(),
            story.getMediaId(),
            story.getCaption(),
            story.getDurationSeconds(),
            story.getCreatedAt(),
            story.getExpiresAt(),
            0, // viewCount sera rempli par StoryWithViews
            false // viewedByMe sera rempli par StoryWithViews
        );
    }

    public static StoryResponse from(StoryService.StoryWithViews storyWithViews) {
        StoryEntity story = storyWithViews.story();
        return new StoryResponse(
            story.getId(),
            story.getAuthorId(),
            story.getMediaId(),
            story.getCaption(),
            story.getDurationSeconds(),
            story.getCreatedAt(),
            story.getExpiresAt(),
            storyWithViews.viewCount(),
            storyWithViews.viewedByMe()
        );
    }
}
