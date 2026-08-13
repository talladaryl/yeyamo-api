package com.yeyamo_mobile.api.recommendation_service.application;

import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yeyamo_mobile.api.recommendation_service.application.port.*;
import com.yeyamo_mobile.api.recommendation_service.application.scoring.*;
import com.yeyamo_mobile.api.recommendation_service.domain.*;

@Service
public class RecommendationQueryService {

    private final RecommendationProjectionPort projections;
    private final RecommendationCachePort cache;
    private final RecommendationOutboxPort outbox;
    private final List<ScoringStrategy> strategies;
    private final ScoringProperties weights;
    private final DiversityPostProcessor diversity;

    @Autowired
    public RecommendationQueryService(
            RecommendationProjectionPort p,
            RecommendationCachePort c,
            RecommendationOutboxPort o,
            List<ScoringStrategy> s,
            ScoringProperties w,
            DiversityPostProcessor d) {
        projections = p; cache = c; outbox = o;
        strategies  = List.copyOf(s); weights = w; diversity = d;
    }

    /** Test / no-diversity constructor. */
    public RecommendationQueryService(
            RecommendationProjectionPort p,
            RecommendationCachePort c,
            RecommendationOutboxPort o,
            List<ScoringStrategy> s) {
        this(p, c, o, s, new ScoringProperties(), new DiversityPostProcessor());
    }

    @Transactional
    public RecommendationPage recommend(
            String user, RecommendationContext context, int page, int size, String correlation) {
        int p = Math.max(0, page);
        int s = Math.max(1, Math.min(50, size));
        return cache.get(user, context, p, s)
                    .orElseGet(() -> generate(user, context, p, s, correlation));
    }

    private RecommendationPage generate(
            String user, RecommendationContext context, int page, int size, String correlation) {

        RecommendationProfile profile = projections.profile(user);

        List<RecommendationItem> ranked = projections.activeCandidates(500).stream()
                .filter(candidate -> matchesCountry(candidate, profile))
                .filter(candidate -> matchesLanguage(candidate, profile, context))
                .map(c -> item(c, profile, context))
                .sorted(Comparator.comparingDouble((RecommendationItem i) -> i.score().total())
                                  .reversed()
                                  .thenComparing(RecommendationItem::targetId))
                .toList();

        // Apply diversity rules — maintains relevance order within each bucket
        List<RecommendationItem> diversified = diversity.apply(ranked, size);

        long offset = (long) page * size;
        int from = (int) Math.min(offset, diversified.size());
        int to   = Math.min(from + size, diversified.size());

        RecommendationPage result = new RecommendationPage(
                page, size, to < diversified.size(),
                List.copyOf(diversified.subList(from, to)),
                Instant.now());

        cache.put(user, context, page, size, result);
        outbox.append("recommendation.generated", user, correlation,
                Map.of("userId", user, "count", result.items().size(),
                       "page", page, "scoringVersion", 2));
        return result;
    }

    private RecommendationItem item(
            Candidate c, RecommendationProfile p, RecommendationContext x) {
        Map<String, Double> parts = new LinkedHashMap<>();
        strategies.forEach(strategy ->
            parts.put(strategy.name(), round(strategy.score(c, p, x) * weights.weight(strategy.name()))));
        double total = round(parts.values().stream().mapToDouble(Double::doubleValue).sum());
        return new RecommendationItem(c.targetId(), c.kind(), c.title(),
                c.categoryCode(), c.regionCode(), c.latitude(), c.longitude(),
                new RecommendationScore(total, parts));
    }

    private double round(double value) { return Math.round(value * 100d) / 100d; }

    private boolean matchesCountry(Candidate candidate, RecommendationProfile profile) {
        if (candidate.countryCode() == null || candidate.countryCode().isBlank()) return true;
        Set<String> countries = new HashSet<>(profile.contentCountries());
        if (profile.countryCode() != null && !profile.countryCode().isBlank()) countries.add(profile.countryCode());
        return countries.isEmpty() || countries.contains(candidate.countryCode());
    }

    private boolean matchesLanguage(Candidate candidate, RecommendationProfile profile, RecommendationContext context) {
        if (candidate.languageCode() == null || candidate.languageCode().isBlank()) return true;
        Set<String> languages = new HashSet<>(profile.contentLanguages());
        languages.addAll(context.languageCodes());
        return languages.isEmpty() || languages.contains(candidate.languageCode());
    }
}
