package com.yeyamo_mobile.api.recommendation_service.infrastructure.persistence;

import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import com.yeyamo_mobile.api.recommendation_service.application.port.RecommendationProjectionPort;
import com.yeyamo_mobile.api.recommendation_service.domain.*;

@Component
public class JpaRecommendationProjectionAdapter implements RecommendationProjectionPort {
    private final CandidateRepository candidates;
    private final PreferenceRepository preferences;
    private final SignalRepository signals;
    private final PendingPopularityRepository pending;

    public JpaRecommendationProjectionAdapter(CandidateRepository c, PreferenceRepository p,
            SignalRepository s, PendingPopularityRepository x) {
        candidates = c; preferences = p; signals = s; pending = x;
    }

    public void upsertCandidate(Candidate c) {
        CandidateEntity e = candidates.findById(c.sourceId()).orElseGet(CandidateEntity::new);
        double popularity = e.sourceId == null ? c.popularity() : e.popularity;
        PendingPopularityEntity wait = pending.findById(c.sourceId()).orElse(null);
        if (wait != null) { popularity += wait.score; pending.delete(wait); }
        e.sourceId = c.sourceId(); e.targetId = c.targetId(); e.kind = c.kind(); e.title = c.title();
        e.categoryCode = c.categoryCode(); e.regionCode = c.regionCode();
        e.countryCode = c.countryCode(); e.languageCode = c.languageCode();
        e.latitude = c.latitude(); e.longitude = c.longitude(); e.popularity = Math.max(0, popularity);
        e.active = c.active(); e.publishedAt = c.publishedAt(); e.updatedAt = c.updatedAt();
        candidates.save(e);
    }

    public void adjustPopularity(String id, double delta) {
        if (candidates.adjust(id, delta, Instant.now()) == 0) {
            PendingPopularityEntity e = pending.findById(id).orElseGet(() -> {
                var n = new PendingPopularityEntity(); n.sourceId = id; return n;
            });
            e.score = Math.max(0, e.score + delta); e.updatedAt = Instant.now(); pending.save(e);
        }
    }

    public void adjustSignal(String user, String source, double delta) {
        SignalId id = new SignalId(user, source);
        SignalEntity e = signals.findById(id).orElseGet(() -> {
            var n = new SignalEntity(); n.id = id; return n;
        });
        e.weight = Math.max(0, e.weight + delta); e.lastInteractionAt = Instant.now(); signals.save(e);
    }

    public void updatePreference(String user, String region, String language, boolean location) {
        PreferenceEntity e = preferences.findById(user).orElseGet(PreferenceEntity::new);
        e.userId = user; e.preferredRegion = region; e.language = language;
        e.locationSharingEnabled = location; e.updatedAt = Instant.now(); preferences.save(e);
    }

    public void updateCountryPreferences(String user, String countryCode,
            Set<String> contentCountries, Set<String> contentLanguages) {
        PreferenceEntity e = preferences.findById(user).orElseGet(PreferenceEntity::new);
        e.userId = user;
        if (countryCode != null) e.countryCode = countryCode.trim().toUpperCase(Locale.ROOT);
        if (contentCountries != null) e.contentCountries = normalize(contentCountries, true);
        if (contentLanguages != null) e.contentLanguages = normalize(contentLanguages, false);
        e.updatedAt = Instant.now();
        preferences.save(e);
    }

    public List<Candidate> activeCandidates(int limit) {
        return candidates.findByActiveTrueOrderByPopularityDescPublishedAtDesc(PageRequest.of(0, limit))
                .stream().map(this::domain).toList();
    }

    public RecommendationProfile profile(String user) {
        PreferenceEntity p = preferences.findById(user).orElse(null);
        Map<String, Double> affinities = new HashMap<>();
        candidates.affinities(user).forEach(row -> affinities.put(String.valueOf(row[0]), ((Number) row[1]).doubleValue()));
        Set<String> seen = new HashSet<>();
        signals.findByIdUserId(user).stream().filter(s -> s.weight > 0).forEach(s -> seen.add(s.id.sourceId));
        return new RecommendationProfile(user, p == null ? null : p.preferredRegion,
                p == null ? null : p.countryCode,
                p == null ? Set.of() : p.contentCountries,
                p == null ? Set.of() : p.contentLanguages,
                p != null && p.locationSharingEnabled, affinities, seen);
    }

    private Candidate domain(CandidateEntity e) {
        return new Candidate(e.sourceId, e.targetId, e.kind, e.title, e.categoryCode, e.regionCode,
                e.countryCode, e.languageCode, e.latitude, e.longitude, e.popularity, e.active,
                e.publishedAt, e.updatedAt);
    }

    private Set<String> normalize(Set<String> values, boolean uppercase) {
        Set<String> normalized = new LinkedHashSet<>();
        values.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isEmpty())
                .map(v -> uppercase ? v.toUpperCase(Locale.ROOT) : v).forEach(normalized::add);
        return normalized;
    }
}
