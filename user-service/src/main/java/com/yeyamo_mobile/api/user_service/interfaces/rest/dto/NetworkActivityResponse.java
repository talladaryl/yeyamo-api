package com.yeyamo_mobile.api.user_service.interfaces.rest.dto;

import java.time.Instant;

import com.yeyamo_mobile.api.user_service.application.SocialGraphService.FollowActivity;

public record NetworkActivityResponse(
        UserProfileSummaryResponse follower,
        UserProfileSummaryResponse followee,
        Instant timestamp,
        String activityType) {
    
    public static NetworkActivityResponse from(FollowActivity activity) {
        return new NetworkActivityResponse(
                UserProfileSummaryResponse.fromBasic(activity.follower()),
                UserProfileSummaryResponse.fromBasic(activity.followee()),
                activity.timestamp(),
                "FOLLOW");
    }
}
