package com.yeyamo_mobile.api.recommendation_service.application.scoring;

import com.yeyamo_mobile.api.recommendation_service.domain.CandidateKind;
import com.yeyamo_mobile.api.recommendation_service.domain.RecommendationItem;
import com.yeyamo_mobile.api.recommendation_service.domain.RecommendationScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DiversityPostProcessorTest {

    private DiversityPostProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new DiversityPostProcessor();
        processor.setMaxConsecutiveSameRegion(2);
        processor.setMaxConsecutiveSameKind(3);
        processor.setMaxRegionShare(0.4);
        processor.setMaxKindShare(0.5);
        processor.setCultureOnly(true);
    }

    // -------------------------------------------------------------------------
    // Happy path — no diversity violations
    // -------------------------------------------------------------------------

    @Test
    void empty_list_returns_empty() {
        assertThat(processor.apply(List.of(), 20)).isEmpty();
    }

    @Test
    void single_item_passes_through() {
        List<RecommendationItem> items = List.of(artwork("ML", 100));
        assertThat(processor.apply(items, 20)).hasSize(1);
    }

    @Test
    void no_region_violations_when_mixed() {
        List<RecommendationItem> items = List.of(
                artwork("ML", 100),
                artwork("SN", 90),
                artwork("GN", 80),
                artwork("ML", 70)
        );
        List<RecommendationItem> result = processor.apply(items, 20);
        // All should appear — ML appears twice which is fine (cap=40% of 4=1.6→2)
        assertThat(result).hasSize(4);
    }

    // -------------------------------------------------------------------------
    // Region cap enforcement
    // -------------------------------------------------------------------------

    @Test
    void region_cap_defers_excess_items() {
        // maxRegionShare=0.4 → cap = ceil(10 * 0.4) = 4 items for same region
        // We push 6 from "ML" and 4 from other regions → ML capped at 4
        List<RecommendationItem> items = new ArrayList<>();
        for (int i = 0; i < 6; i++) items.add(artwork("ML", 100 - i));
        for (int i = 0; i < 4; i++) items.add(artwork("SN", 50 - i));

        List<RecommendationItem> result = processor.apply(items, 10);

        long mlCount = result.stream().filter(r -> "ML".equals(r.regionCode())).count();
        // At most 4 ML items in first 10 positions (the rest are deferred, still present)
        assertThat(result).hasSize(10);
        assertThat(mlCount).isEqualTo(4);
    }

    // -------------------------------------------------------------------------
    // Consecutive region cap
    // -------------------------------------------------------------------------

    @Test
    void consecutive_same_region_enforced() {
        // maxConsecutiveSameRegion=2
        List<RecommendationItem> items = List.of(
                artwork("ML", 100),
                artwork("ML", 90),
                artwork("ML", 80),  // 3rd consecutive ML — should be deferred
                artwork("SN", 70)
        );

        List<RecommendationItem> result = processor.apply(items, 20);

        // Items are: ML, ML, SN, ML(deferred)
        assertThat(result.get(0).regionCode()).isEqualTo("ML");
        assertThat(result.get(1).regionCode()).isEqualTo("ML");
        assertThat(result.get(2).regionCode()).isEqualTo("SN");
        // 3rd ML is deferred but still in the list
        assertThat(result).hasSize(4);
    }

    // -------------------------------------------------------------------------
    // Culture-only flag
    // -------------------------------------------------------------------------

    @Test
    void non_culture_kinds_bypass_diversity() {
        processor.setCultureOnly(true);

        // 10 CONTENT items from same region — should NOT be diversity-filtered
        List<RecommendationItem> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) items.add(content("ML", 100 - i));

        List<RecommendationItem> result = processor.apply(items, 10);
        assertThat(result).hasSize(10);
    }

    @Test
    void culture_only_false_applies_to_all_kinds() {
        processor.setCultureOnly(false);
        processor.setMaxConsecutiveSameRegion(2);

        List<RecommendationItem> items = List.of(
                content("ML", 100),
                content("ML", 90),
                content("ML", 80),  // deferred
                content("SN", 70)
        );

        List<RecommendationItem> result = processor.apply(items, 20);
        assertThat(result.get(2).regionCode()).isEqualTo("SN");
    }

    // -------------------------------------------------------------------------
    // Kind cap enforcement
    // -------------------------------------------------------------------------

    @Test
    void kind_cap_defers_excess_artworks() {
        // maxKindShare=0.5 → cap = ceil(10 * 0.5) = 5 artworks
        List<RecommendationItem> items = new ArrayList<>();
        for (int i = 0; i < 8; i++) items.add(artwork("ML", 100 - i));  // 8 artworks
        for (int i = 0; i < 2; i++) items.add(artisan("SN", 50 - i));   // 2 artisans

        List<RecommendationItem> result = processor.apply(items, 10);

        long artworkCount = result.stream().filter(r -> r.kind() == CandidateKind.ARTWORK).count();
        assertThat(result).hasSize(10);
        assertThat(artworkCount).isEqualTo(5);
    }

    // -------------------------------------------------------------------------
    // No items lost
    // -------------------------------------------------------------------------

    @Test
    void total_item_count_preserved_after_diversity() {
        List<RecommendationItem> items = new ArrayList<>();
        for (int i = 0; i < 20; i++) items.add(artwork("ML", 100 - i));
        for (int i = 0; i < 5; i++)  items.add(artisan("SN", 50 - i));

        List<RecommendationItem> result = processor.apply(items, 20);
        assertThat(result).hasSize(25); // deferred items appended after main pass
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static RecommendationItem artwork(String region, double score) {
        return new RecommendationItem("id-" + score, CandidateKind.ARTWORK, "Artwork " + score,
                null, region, null, null, new RecommendationScore(score, Map.of()));
    }

    private static RecommendationItem artisan(String region, double score) {
        return new RecommendationItem("id-" + score, CandidateKind.ARTISAN, "Artisan " + score,
                null, region, null, null, new RecommendationScore(score, Map.of()));
    }

    private static RecommendationItem content(String region, double score) {
        return new RecommendationItem("id-" + score, CandidateKind.CONTENT, "Content " + score,
                null, region, null, null, new RecommendationScore(score, Map.of()));
    }
}
