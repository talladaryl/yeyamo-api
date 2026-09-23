package com.yeyamo_mobile.api.recommendation_service.domain;

/** Candidate plus the score computed by the existing Recommendation scoring strategies. */
public record AdventureRankedCandidate(Candidate candidate, RecommendationScore score) {
}
