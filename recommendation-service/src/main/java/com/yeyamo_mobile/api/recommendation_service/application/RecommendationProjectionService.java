package com.yeyamo_mobile.api.recommendation_service.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.recommendation_service.application.port.RecommendationCachePort;
import com.yeyamo_mobile.api.recommendation_service.application.port.RecommendationProjectionPort;
import com.yeyamo_mobile.api.recommendation_service.domain.Candidate;

@Service
public class RecommendationProjectionService {
    /** Existing favourite signal weight: feedback deliberately reuses it. */
    private static final double FAVORITE_SIGNAL_WEIGHT = 3d;
    private final RecommendationProjectionPort port;
    private final RecommendationCachePort cache;
    public RecommendationProjectionService(RecommendationProjectionPort port, RecommendationCachePort cache) { this.port = port; this.cache = cache; }
    @Transactional public void candidate(Candidate candidate) { port.upsertCandidate(candidate); cache.invalidate(); }
    @Transactional public void popularity(String source, double delta) { port.adjustPopularity(source, delta); cache.invalidate(); }
    @Transactional public void signal(String user, String source, double delta) { if (user != null && !user.isBlank()) port.adjustSignal(user, source, delta); cache.invalidate(); }
    @Transactional public void preference(String user, String region, String language, boolean location) { port.updatePreference(user, region, language, location); cache.invalidate(); }
    @Transactional public void countryPreferences(String user, String country, java.util.Set<String> countries, java.util.Set<String> languages) { port.updateCountryPreferences(user, country, countries, languages); cache.invalidate(); }

    @Transactional
    public void feedback(String user, String targetType, String targetId, String feedbackType, String previous) {
        if ("INTERESTED".equals(previous)) signalFor(user, targetType, targetId, -FAVORITE_SIGNAL_WEIGHT);
        port.upsertFeedback(user, targetType, targetId, feedbackType);
        if ("INTERESTED".equals(feedbackType)) signalFor(user, targetType, targetId, FAVORITE_SIGNAL_WEIGHT);
        cache.invalidate();
    }

    @Transactional
    public void removeFeedback(String user, String targetType, String targetId, String previous) {
        if ("INTERESTED".equals(previous)) signalFor(user, targetType, targetId, -FAVORITE_SIGNAL_WEIGHT);
        port.removeFeedback(user, targetType, targetId);
        cache.invalidate();
    }

    private void signalFor(String user, String targetType, String targetId, double delta) {
        port.candidatesForTargetId(targetId).stream()
                .filter(candidate -> targetType.equals(targetType(candidate)))
                .forEach(candidate -> port.adjustSignal(user, candidate.sourceId(), delta));
    }

    private String targetType(Candidate candidate) {
        return switch (candidate.kind()) {
            case CONTENT -> "POST";
            case EVENT -> "EVENT";
            case PLACE, DESTINATION -> "PLACE";
            case EXPERIENCE -> "ACTIVITY";
            case CULTURE, LANGUAGE, TRADITION -> "CULTURE_CONTENT";
            default -> candidate.kind().name();
        };
    }
}
