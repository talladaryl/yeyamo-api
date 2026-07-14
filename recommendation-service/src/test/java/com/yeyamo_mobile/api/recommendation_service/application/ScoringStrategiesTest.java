package com.yeyamo_mobile.api.recommendation_service.application;
import static org.junit.jupiter.api.Assertions.*;import java.time.Instant;import java.util.*;import org.junit.jupiter.api.Test;import com.yeyamo_mobile.api.recommendation_service.application.scoring.*;import com.yeyamo_mobile.api.recommendation_service.domain.*;
class ScoringStrategiesTest{
 private final Candidate candidate=new Candidate("catalog:1","1",CandidateKind.PLACE,"Plage","BEACH","10",4d,9d,20,true,Instant.now(),Instant.now());
 @Test void popularityIsPositiveAndBounded(){double score=new PopularityScoringStrategy().score(candidate,profile(false),new RecommendationContext(null,null));assertTrue(score>0&&score<=30);}
 @Test void proximityRequiresLocationConsent(){var strategy=new ProximityScoringStrategy();assertEquals(0,strategy.score(candidate,profile(false),new RecommendationContext(4d,9d)));assertEquals(25,strategy.score(candidate,profile(true),new RecommendationContext(4d,9d)),0.01);}
 @Test void preferencesCombineRegionAndCategoryAffinity(){assertEquals(23,new PreferenceScoringStrategy().score(candidate,profile(true),new RecommendationContext(null,null)));}
 @Test void historyPenalizesSeenCandidate(){assertEquals(-30,new HistoryScoringStrategy().score(candidate,profile(true),new RecommendationContext(null,null)));}
 private RecommendationProfile profile(boolean location){return new RecommendationProfile("u","10",location,Map.of("BEACH",4d),Set.of("catalog:1"));}
}
