package com.yeyamo_mobile.api.recommendation_service.domain;import java.util.Map;
public record RecommendationScore(double total,Map<String,Double>components){}
