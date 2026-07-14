package com.yeyamo_mobile.api.recommendation_service.domain;
public record RecommendationItem(String targetId,CandidateKind kind,String title,String categoryCode,String regionCode,Double latitude,Double longitude,RecommendationScore score){}
