package com.yeyamo_mobile.api.feed_service.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Feed item - can be organic content or sponsored placement
 */
public record FeedItem(
    // Common fields
    String itemType,  // "ORGANIC" or "SPONSORED"
    
    // Organic content fields (present when itemType=ORGANIC)
    UUID postId,
    String authorId,
    String caption,
    UUID catalogAssetId,
    String cardType,
    String referenceType,
    String referenceId,
    List<UUID> mediaIds,
    List<String> hashtags,
    Instant publishedAt,
    long likes,
    long comments,
    long shares,
    double rankingScore,
    
    // Sponsored content fields (present when itemType=SPONSORED)
    String deliveryId,
    String campaignId,
    String promotedEntityType,
    String promotedEntityId,
    Map<String, Object> creative,
    BigDecimal bidAmount,
    String trackingToken
) {
    
    /**
     * Create organic feed item (backward compatible)
     */
    public static FeedItem organic(
            UUID postId,
            String authorId,
            String caption,
            UUID catalogAssetId,
            List<UUID> mediaIds,
            List<String> hashtags,
            Instant publishedAt,
            long likes,
            long comments,
            long shares,
            double rankingScore) {
        return organic(postId,authorId,caption,catalogAssetId,catalogAssetId==null?"SOCIAL":"PLACE",catalogAssetId==null?"NONE":"PLACE",catalogAssetId==null?null:catalogAssetId.toString(),mediaIds,hashtags,publishedAt,likes,comments,shares,rankingScore);
    }

    public static FeedItem organic(UUID postId,String authorId,String caption,UUID catalogAssetId,String cardType,String referenceType,String referenceId,List<UUID>mediaIds,List<String>hashtags,Instant publishedAt,long likes,long comments,long shares,double rankingScore) {
        return new FeedItem(
            "ORGANIC",
            postId, authorId, caption, catalogAssetId, cardType, referenceType, referenceId, mediaIds, hashtags,
            publishedAt, likes, comments, shares, rankingScore,
            null, null, null, null, null, null, null
        );
    }
    
    /**
     * Create sponsored feed item
     */
    public static FeedItem sponsored(
            String deliveryId,
            String campaignId,
            String promotedEntityType,
            String promotedEntityId,
            Map<String, Object> creative,
            BigDecimal bidAmount,
            String trackingToken) {
        
        return new FeedItem(
            "SPONSORED",
            null, null, null, null, null, null, null, null, null, null, 0, 0, 0, 0,
            deliveryId, campaignId, promotedEntityType, promotedEntityId,
            creative, bidAmount, trackingToken
        );
    }
    
    public boolean isSponsored() {
        return "SPONSORED".equals(itemType);
    }
    
    public boolean isOrganic() {
        return "ORGANIC".equals(itemType);
    }
}
