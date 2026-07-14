package com.yeyamo_mobile.api.feed_service.application;import java.time.Instant;import java.util.*;
public record FeedItem(UUID postId,String authorId,String caption,UUID catalogAssetId,List<UUID>mediaIds,List<String>hashtags,Instant publishedAt,long likes,long comments,long shares,double rankingScore){}
