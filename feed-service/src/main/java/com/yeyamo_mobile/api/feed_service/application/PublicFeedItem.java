package com.yeyamo_mobile.api.feed_service.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.yeyamo_mobile.api.feed_service.domain.model.FeedMetric;
import com.yeyamo_mobile.api.feed_service.domain.model.FeedPost;

public record PublicFeedItem(UUID postId, String authorId, String caption, List<UUID> mediaIds,
        List<String> hashtags, Instant publishedAt, long likes, long comments, long shares,
        String referenceType, String referenceId) {
    static PublicFeedItem from(FeedPost post, FeedMetric metric) {
        return new PublicFeedItem(post.postId(), post.authorId(), post.caption(), post.mediaIds(), post.hashtags(),
                post.publishedAt(), metric.likes(), metric.comments(), metric.shares(), post.referenceType(), post.referenceId());
    }
}
