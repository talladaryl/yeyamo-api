package com.yeyamo_mobile.api.recommendation_service.application;import java.time.Instant;import java.util.List;import com.yeyamo_mobile.api.recommendation_service.domain.RecommendationItem;
public record RecommendationPage(int page,int size,boolean hasNext,List<RecommendationItem>items,Instant generatedAt){}
