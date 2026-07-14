package com.yeyamo_mobile.api.mission_reward_service.domain;
import static org.junit.jupiter.api.Assertions.*;import java.time.Instant;import java.util.*;import org.junit.jupiter.api.Test;
class ProgressCalculatorTest{
 private MissionEvent event(Map<String,Object>payload){return new MissionEvent(UUID.randomUUID(),"interaction.checkin.created","u1",payload,Instant.now(),"c1");}
 @Test void countsMatchingEvents(){var rule=new ObjectiveRule(ObjectiveMetric.COUNT,3,null,"country","CM");assertEquals(2,ProgressCalculator.next(rule,1,event(Map.of("country","CM"))));}
 @Test void ignoresEventsFailingRule(){var rule=new ObjectiveRule(ObjectiveMetric.COUNT,3,null,"country","CM");assertEquals(1,ProgressCalculator.next(rule,1,event(Map.of("country","FR"))));}
 @Test void sumsAndCapsAtTarget(){var rule=new ObjectiveRule(ObjectiveMetric.SUM,100,"points",null,null);assertEquals(100,ProgressCalculator.next(rule,80,event(Map.of("points",30))));}
 @Test void keepsMaximumValue(){var rule=new ObjectiveRule(ObjectiveMetric.MAX,5,"level",null,null);assertEquals(4,ProgressCalculator.next(rule,4,event(Map.of("level",3))));assertEquals(5,ProgressCalculator.next(rule,4,event(Map.of("level",6))));}
 @Test void rejectsMissingNumericValue(){var rule=new ObjectiveRule(ObjectiveMetric.SUM,10,"points",null,null);assertThrows(IllegalArgumentException.class,()->ProgressCalculator.next(rule,0,event(Map.of())));}
}
