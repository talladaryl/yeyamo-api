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
    String countryCode,
    UUID adminLevel1Id,
    UUID adminLevel2Id,
    UUID cityId,
    UUID localityId,
    Double latitude,
    Double longitude,
    String languageCode,
    Instant createdAt,
    Instant expiresAt,
    long viewCount,
    boolean viewedByMe
) {
    public static StoryResponse from(StoryEntity story) {
        return from(story, 0, false);
    }

    public static StoryResponse from(StoryEntity story, long viewCount, boolean viewedByMe) {
        return new StoryResponse(
            story.getId(),
            story.getAuthorId(),
            story.getMediaId(),
            story.getCaption(),
            story.getDurationSeconds(),
            story.getGeography() == null ? null : story.getGeography().getCountryCode(),
            story.getGeography() == null ? null : story.getGeography().getAdminLevel1Id(),
            story.getGeography() == null ? null : story.getGeography().getAdminLevel2Id(),
            story.getGeography() == null ? null : story.getGeography().getCityId(),
            story.getGeography() == null ? null : story.getGeography().getLocalityId(),
            story.getGeography() == null ? null : story.getGeography().getLatitude(),
            story.getGeography() == null ? null : story.getGeography().getLongitude(),
            story.getGeography() == null ? null : story.getGeography().getLanguageCode(),
            story.getCreatedAt(),
            story.getExpiresAt(),
            viewCount,
            viewedByMe
        );
    }

    public static StoryResponse from(StoryService.StoryWithViews storyWithViews) {
        return from(storyWithViews.story(), storyWithViews.viewCount(), storyWithViews.viewedByMe());
    }
}
