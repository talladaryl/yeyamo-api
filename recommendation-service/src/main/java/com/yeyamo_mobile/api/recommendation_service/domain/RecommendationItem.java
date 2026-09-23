package com.yeyamo_mobile.api.recommendation_service.domain;
public record RecommendationItem(String targetId,CandidateKind kind,String title,String categoryCode,String regionCode,Double latitude,Double longitude,RecommendationScore score,RecommendationViewerState viewerState){
    public RecommendationItem(String targetId,CandidateKind kind,String title,String categoryCode,String regionCode,Double latitude,Double longitude,RecommendationScore score){this(targetId,kind,title,categoryCode,regionCode,latitude,longitude,score,RecommendationViewerState.NONE);}
}
