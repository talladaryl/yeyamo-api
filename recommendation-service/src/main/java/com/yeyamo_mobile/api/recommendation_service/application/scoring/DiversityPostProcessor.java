package com.yeyamo_mobile.api.recommendation_service.application.scoring;

import com.yeyamo_mobile.api.recommendation_service.domain.CandidateKind;
import com.yeyamo_mobile.api.recommendation_service.domain.RecommendationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Post-processes a scored, sorted recommendation list to enforce diversity rules.
 *
 * <p>Purpose: prevent a single culture, region, or content kind from dominating
 * the feed while still respecting relevance ordering.</p>
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Walk the sorted list from top to bottom.</li>
 *   <li>For each item, check if any cap is exceeded for its region or kind.</li>
 *   <li>If capped, defer it to a "spillover" queue.</li>
 *   <li>Once the main pass is complete, append deferred items.</li>
 * </ol>
 * This preserves relevance within each bucket and guarantees no item is lost.
 * </p>
 *
 * <p>All thresholds are configurable via
 * {@code recommendation.diversity.*} application properties so product teams
 * can tune them without code changes.</p>
 */
@Component
@ConfigurationProperties(prefix = "recommendation.diversity")
public class DiversityPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DiversityPostProcessor.class);

    /** Max consecutive items from the same region before enforcing diversity. */
    private int maxConsecutiveSameRegion = 3;

    /** Max consecutive items of the same CandidateKind before enforcing diversity. */
    private int maxConsecutiveSameKind = 4;

    /**
     * Max share of a single region within a page (0–1).
     * Default 0.5 = no region may occupy more than 50% of a page.
     */
    private double maxRegionShare = 0.50;

    /**
     * Max share of a single CandidateKind within a page (0–1).
     * Default 0.6 = no kind may occupy more than 60% of a page.
     */
    private double maxKindShare = 0.60;

    /** If true, diversity rules apply only to culture-related kinds. */
    private boolean cultureOnly = true;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public List<RecommendationItem> apply(List<RecommendationItem> sorted, int pageSize) {
        if (sorted == null || sorted.size() <= 1) return sorted;

        int cap = Math.min(pageSize, sorted.size());
        int regionCap = (int) Math.ceil(cap * maxRegionShare);
        int kindCap   = (int) Math.ceil(cap * maxKindShare);

        Map<String, Integer> regionCount = new HashMap<>();
        Map<CandidateKind, Integer> kindCount = new HashMap<>();

        List<RecommendationItem> result = new ArrayList<>(cap);
        List<RecommendationItem> deferred = new ArrayList<>();

        String lastRegion = null;
        CandidateKind lastKind = null;
        int consecutiveRegion = 0;
        int consecutiveKind   = 0;

        for (RecommendationItem item : sorted) {
            if (!shouldApply(item)) {
                result.add(item);
                // reset consecutive counters — non-culture item breaks the streak
                consecutiveRegion = 0;
                consecutiveKind   = 0;
                lastRegion = null;
                lastKind   = null;
                continue;
            }

            String region = item.regionCode();
            CandidateKind kind = item.kind();

            boolean regionExceeded = region != null
                    && (regionCount.getOrDefault(region, 0) >= regionCap
                        || (region.equals(lastRegion) && consecutiveRegion >= maxConsecutiveSameRegion));

            boolean kindExceeded = kind != null
                    && (kindCount.getOrDefault(kind, 0) >= kindCap
                        || (kind.equals(lastKind) && consecutiveKind >= maxConsecutiveSameKind));

            if (regionExceeded || kindExceeded) {
                deferred.add(item);
                log.debug("Diversity: deferred item kind={} region={} (regionExceeded={}, kindExceeded={})",
                          kind, region, regionExceeded, kindExceeded);
                continue;
            }

            result.add(item);
            if (region != null) {
                regionCount.merge(region, 1, Integer::sum);
                consecutiveRegion = region.equals(lastRegion) ? consecutiveRegion + 1 : 1;
            } else {
                consecutiveRegion = 0;
            }
            if (kind != null) {
                kindCount.merge(kind, 1, Integer::sum);
                consecutiveKind = kind.equals(lastKind) ? consecutiveKind + 1 : 1;
            } else {
                consecutiveKind = 0;
            }
            lastRegion = region;
            lastKind   = kind;
        }

        // Append deferred items — still shown, just pushed down the list
        result.addAll(deferred);

        if (!deferred.isEmpty()) {
            log.debug("Diversity post-processor: {} items deferred out of {}", deferred.size(), sorted.size());
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean shouldApply(RecommendationItem item) {
        if (!cultureOnly) return true;
        return item.kind() != null && switch (item.kind()) {
            case ARTWORK, ARTISAN, CULTURE, LANGUAGE, TRADITION -> true;
            default -> false;
        };
    }

    // -------------------------------------------------------------------------
    // Getters / setters (required by @ConfigurationProperties)
    // -------------------------------------------------------------------------

    public int getMaxConsecutiveSameRegion()            { return maxConsecutiveSameRegion; }
    public void setMaxConsecutiveSameRegion(int v)      { maxConsecutiveSameRegion = Math.max(1, v); }

    public int getMaxConsecutiveSameKind()              { return maxConsecutiveSameKind; }
    public void setMaxConsecutiveSameKind(int v)        { maxConsecutiveSameKind = Math.max(1, v); }

    public double getMaxRegionShare()                   { return maxRegionShare; }
    public void setMaxRegionShare(double v)             { maxRegionShare = clamp01(v); }

    public double getMaxKindShare()                     { return maxKindShare; }
    public void setMaxKindShare(double v)               { maxKindShare = clamp01(v); }

    public boolean isCultureOnly()                      { return cultureOnly; }
    public void setCultureOnly(boolean v)               { cultureOnly = v; }

    private double clamp01(double v) {
        if (!Double.isFinite(v) || v <= 0) throw new IllegalArgumentException("Share must be > 0");
        return Math.min(1.0, v);
    }
}
