package com.yeyamo_mobile.api.recommendation_service.application.scoring;

import com.yeyamo_mobile.api.recommendation_service.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CultureScoringStrategyTest {

    private CultureScoringStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new CultureScoringStrategy();
    }

    @Test
    void non_culture_kind_returns_zero() {
        Candidate c = candidate(CandidateKind.CONTENT, "ML", null);
        assertThat(strategy.score(c, profile("ML"), ctx(null, null, null))).isZero();
    }

    @Test
    void artwork_with_matching_region_scores_positively() {
        Candidate c = candidate(CandidateKind.ARTWORK, "ML", null);
        double score = strategy.score(c, profile("ML"), ctx(null, null, null));
        assertThat(score).isGreaterThan(0);
    }

    @Test
    void language_kind_with_matching_language_code_scores_bonus() {
        Candidate c = candidate(CandidateKind.LANGUAGE, null, "bm"); // bambara
        RecommendationContext ctx = ctx(null, List.of("bm", "fr"), null);
        double score = strategy.score(c, profile(null), ctx);
        assertThat(score).isGreaterThanOrEqualTo(10);
    }

    @Test
    void artworks_context_boosts_artwork_kind() {
        Candidate c = candidate(CandidateKind.ARTWORK, null, null);
        double score = strategy.score(c, profile(null), ctx(null, null, "artworks"));
        assertThat(score).isGreaterThanOrEqualTo(10);
    }

    @Test
    void daily_learning_context_boosts_language_more_than_culture() {
        Candidate lang    = candidate(CandidateKind.LANGUAGE, null, null);
        Candidate culture = candidate(CandidateKind.CULTURE,  null, null);
        RecommendationContext ctx = ctx(null, null, "daily_learning");

        double langScore    = strategy.score(lang,    profile(null), ctx);
        double cultureScore = strategy.score(culture, profile(null), ctx);

        assertThat(langScore).isGreaterThan(cultureScore);
    }

    @Test
    void score_capped_at_35() {
        // region match (15) + language match (10) + context match (10) = 35
        Candidate c = candidate(CandidateKind.LANGUAGE, "ML", "bm");
        RecommendationContext ctx = ctx(null, List.of("bm"), "languages");
        double score = strategy.score(c, profile("ML"), ctx);
        assertThat(score).isEqualTo(35.0);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Candidate candidate(CandidateKind kind, String region, String categoryCode) {
        return new Candidate("s:" + kind, "t:" + kind, kind, "Title",
                categoryCode, region, null, null, 10, true, Instant.now(), Instant.now());
    }

    private RecommendationProfile profile(String region) {
        return new RecommendationProfile("user-1", region, false, Map.of(), Set.of());
    }

    private RecommendationContext ctx(Double lat, List<String> langs, String context) {
        return new RecommendationContext(lat, lat == null ? null : 0.0,
                langs == null ? List.of() : langs, context);
    }
}
