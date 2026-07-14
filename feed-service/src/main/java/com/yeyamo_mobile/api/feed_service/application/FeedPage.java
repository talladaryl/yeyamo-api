package com.yeyamo_mobile.api.feed_service.application;import java.time.Instant;import java.util.List;public record FeedPage(String userId,int page,int size,List<FeedItem>items,Instant generatedAt){}
