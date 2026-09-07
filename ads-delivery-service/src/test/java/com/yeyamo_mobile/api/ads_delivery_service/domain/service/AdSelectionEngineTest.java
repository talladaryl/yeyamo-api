package com.yeyamo_mobile.api.ads_delivery_service.domain.service;

import com.yeyamo_mobile.api.ads_delivery_service.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AdSelectionEngineTest {

    private AdSelectionEngine engine;
    private AdSelectionContext context;

    @BeforeEach
    void setUp() {
        // The production engine may explore a different eligible campaign in 5% of requests.
        // A seeded source keeps the score-order assertion deterministic.
        engine = new AdSelectionEngine(new Random(0L));
        
        context = AdSelectionContext.builder()
            .placement(PlacementType.FEED_CARD)
            .countryCode("CM")
            .regionId("CM-CE")
            .cityId("YAO")
            .interestIds(List.of("INT1", "INT2"))
            .language("fr")
            .deviceType("MOBILE")
            .requestTimestamp(Instant.now())
            .limit(10)
            .build();
    }

    @Test
    void shouldSelectEligibleCampaigns() {
        // Given
        CampaignProjection campaign = createTestCampaign("CAMP1");
        List<CampaignProjection> campaigns = List.of(campaign);

        // When
        List<ScoredAd> results = engine.selectAds(campaigns, context);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("CAMP1", results.get(0).getCampaign().getCampaignId());
    }

    @Test
    void shouldFilterInactiveCampaigns() {
        // Given
        CampaignProjection campaign = CampaignProjection.builder()
            .campaignId("CAMP1")
            .partnerId("PARTNER1")
            .name("Test Campaign")
            .objective("AWARENESS")
            .promotedEntityType(PromotedEntityType.PLACE)
            .promotedEntityId("PLACE1")
            .billingModel("CPM")
            .bidAmount(BigDecimal.valueOf(100))
            .totalBudget(BigDecimal.valueOf(10000))
            .dailyBudget(BigDecimal.valueOf(1000))
            .spentAmount(BigDecimal.ZERO)
            .currency("XAF")
            .startAt(Instant.now().plusSeconds(86400)) // Starts tomorrow
            .endAt(Instant.now().plusSeconds(172800))
            .eligiblePlacements(List.of(PlacementType.FEED_CARD))
            .targetCountries(List.of("CM"))
            .targetLanguages(List.of("fr"))
            .creativeJson("{}")
            .qualityScore(80)
            .createdAt(Instant.now())
            .build();

        List<CampaignProjection> campaigns = List.of(campaign);

        // When
        List<ScoredAd> results = engine.selectAds(campaigns, context);

        // Then
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldFilterNoBudgetCampaigns() {
        // Given
        CampaignProjection campaign = CampaignProjection.builder()
            .campaignId("CAMP1")
            .partnerId("PARTNER1")
            .name("Test Campaign")
            .objective("AWARENESS")
            .promotedEntityType(PromotedEntityType.PLACE)
            .promotedEntityId("PLACE1")
            .billingModel("CPM")
            .bidAmount(BigDecimal.valueOf(100))
            .totalBudget(BigDecimal.valueOf(10000))
            .dailyBudget(BigDecimal.valueOf(1000))
            .spentAmount(BigDecimal.valueOf(10000)) // Budget exhausted
            .currency("XAF")
            .startAt(Instant.now().minusSeconds(86400))
            .endAt(Instant.now().plusSeconds(86400))
            .eligiblePlacements(List.of(PlacementType.FEED_CARD))
            .targetCountries(List.of("CM"))
            .targetLanguages(List.of("fr"))
            .creativeJson("{}")
            .qualityScore(80)
            .createdAt(Instant.now())
            .build();

        List<CampaignProjection> campaigns = List.of(campaign);

        // When
        List<ScoredAd> results = engine.selectAds(campaigns, context);

        // Then
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldSortByScore() {
        // Given
        CampaignProjection campaign1 = createTestCampaignWithBid("CAMP1", BigDecimal.valueOf(100));
        CampaignProjection campaign2 = createTestCampaignWithBid("CAMP2", BigDecimal.valueOf(500));
        List<CampaignProjection> campaigns = List.of(campaign1, campaign2);

        // When
        List<ScoredAd> results = engine.selectAds(campaigns, context);

        // Then
        assertEquals(2, results.size());
        // Higher bid should be first (assuming other factors equal)
        assertTrue(results.get(0).getScore() >= results.get(1).getScore());
    }

    @Test
    void shouldRespectLimit() {
        // Given
        List<CampaignProjection> campaigns = List.of(
            createTestCampaign("CAMP1"),
            createTestCampaign("CAMP2"),
            createTestCampaign("CAMP3")
        );

        AdSelectionContext limitedContext = AdSelectionContext.builder()
            .placement(PlacementType.FEED_CARD)
            .countryCode("CM")
            .language("fr")
            .deviceType("MOBILE")
            .requestTimestamp(Instant.now())
            .limit(2)
            .build();

        // When
        List<ScoredAd> results = engine.selectAds(campaigns, limitedContext);

        // Then
        assertEquals(2, results.size());
    }

    @Test
    void shouldGenerateReasonCodes() {
        // Given
        CampaignProjection campaign = createTestCampaign("CAMP1");
        List<CampaignProjection> campaigns = List.of(campaign);

        // When
        List<ScoredAd> results = engine.selectAds(campaigns, context);

        // Then
        assertFalse(results.get(0).getReasonCodes().isEmpty());
    }

    private CampaignProjection createTestCampaign(String id) {
        return createTestCampaignWithBid(id, BigDecimal.valueOf(100));
    }

    private CampaignProjection createTestCampaignWithBid(String id, BigDecimal bidAmount) {
        return CampaignProjection.builder()
            .campaignId(id)
            .partnerId("PARTNER1")
            .name("Test Campaign")
            .objective("AWARENESS")
            .promotedEntityType(PromotedEntityType.PLACE)
            .promotedEntityId("PLACE1")
            .billingModel("CPM")
            .bidAmount(bidAmount)
            .totalBudget(BigDecimal.valueOf(10000))
            .dailyBudget(BigDecimal.valueOf(1000))
            .spentAmount(BigDecimal.ZERO)
            .currency("XAF")
            .startAt(Instant.now().minusSeconds(86400))
            .endAt(Instant.now().plusSeconds(86400))
            .eligiblePlacements(List.of(PlacementType.FEED_CARD))
            .targetCountries(List.of("CM"))
            .targetCities(List.of("YAO"))
            .targetInterests(List.of("INT1"))
            .targetLanguages(List.of("fr"))
            .creativeJson("{}")
            .qualityScore(80)
            .createdAt(Instant.now().minusSeconds(3600))
            .build();
    }
}
