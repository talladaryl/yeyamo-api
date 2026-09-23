package com.yeyamo_mobile.api.event_service.dto;

import java.util.UUID;

import com.yeyamo_mobile.api.event_service.enums.SocialDistributionStatus;

public record SocialDistributionResponse(
        boolean publishToFeed,
        boolean publishToStory,
        SocialDistributionStatus feedStatus,
        SocialDistributionStatus storyStatus,
        UUID feedPostId,
        UUID storyId,
        String feedReason,
        String storyReason) {
}
