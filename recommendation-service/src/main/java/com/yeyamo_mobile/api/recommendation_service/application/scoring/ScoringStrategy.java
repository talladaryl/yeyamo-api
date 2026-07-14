package com.yeyamo_mobile.api.recommendation_service.application.scoring;import com.yeyamo_mobile.api.recommendation_service.domain.*;
public interface ScoringStrategy{String name();double score(Candidate candidate,RecommendationProfile profile,RecommendationContext context);}
