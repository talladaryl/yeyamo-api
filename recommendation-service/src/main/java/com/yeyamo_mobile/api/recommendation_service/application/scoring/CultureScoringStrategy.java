package com.yeyamo_mobile.api.recommendation_service.application.scoring;

import com.yeyamo_mobile.api.recommendation_service.domain.Candidate;
import com.yeyamo_mobile.api.recommendation_service.domain.CandidateKind;
import com.yeyamo_mobile.api.recommendation_service.domain.RecommendationContext;
import com.yeyamo_mobile.api.recommendation_service.domain.RecommendationProfile;
import org.springframework.stereotype.Component;

/**
 * Scoring strategy that boosts culture, language, artisan, and artwork candidates
 * based on the user's cultural context signals.
 *
 * <p>Signals used:
 * <ul>
 *   <li>Region / country match — strong locality signal</li>
 *   <li>Language code match against {@code context.languageCodes}</li>
 *   <li>Category affinity already captured by {@link PreferenceScoringStrategy}
 *       — this strategy adds culture-specific bonuses on top</li>
 *   <li>Content type preference (heritage, daily_learning …)</li>
 * </ul>
 * </p>
 *
 * <p>Score range: 0–35 (no penalty applied here; history strategy handles penalties).</p>
 */
@Component
public class CultureScoringStrategy implements ScoringStrategy {

    @Override
    public String name() {
        return "culture";
    }

    @Override
    public double score(Candidate candidate, RecommendationProfile profile, RecommendationContext context) {
        if (!isCultureKind(candidate.kind())) return 0;

        double score = 0;

        // -- region / country affinity (15 pts) --------------------------------
        if (candidate.regionCode() != null
                && candidate.regionCode().equals(profile.preferredRegion())) {
            score += 15;
        }

        // -- language match (10 pts) -------------------------------------------
        if (context.languageCodes() != null && !context.languageCodes().isEmpty()
                && candidate.categoryCode() != null) {
            // categoryCode is reused as languageCode for LANGUAGE kind
            if (context.languageCodes().contains(candidate.categoryCode())) {
                score += 10;
            }
        }

        // -- content-type preference bonus (10 pts) ----------------------------
        String preferredContext = context.recommendationContext();
        if (preferredContext != null) {
            score += switch (preferredContext) {
                case "culture"        -> isCultureContent(candidate) ? 10 : 0;
                case "artworks"       -> candidate.kind() == CandidateKind.ARTWORK  ? 10 : 0;
                case "artisan"        -> candidate.kind() == CandidateKind.ARTISAN  ? 10 : 0;
                case "languages"      -> candidate.kind() == CandidateKind.LANGUAGE ? 10 : 0;
                case "heritage"       -> candidate.kind() == CandidateKind.TRADITION ? 10 : 0;
                case "daily_learning" -> candidate.kind() == CandidateKind.LANGUAGE ? 10 : 5;
                default               -> 0;
            };
        }

        return Math.min(35, score);
    }

    // -------------------------------------------------------------------------

    private boolean isCultureKind(CandidateKind kind) {
        return switch (kind) {
            case ARTWORK, ARTISAN, CULTURE, LANGUAGE, TRADITION -> true;
            default -> false;
        };
    }

    private boolean isCultureContent(Candidate c) {
        return c.kind() == CandidateKind.CULTURE || c.kind() == CandidateKind.TRADITION;
    }
}
