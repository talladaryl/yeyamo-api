package com.yeyamo_mobile.api.discovery_service.application;
import static org.mockito.Mockito.*;import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.discovery_service.application.port.*;
class DiscoveryProjectionServiceTest {
 @Test void appliesTrendWeightsAndInvalidatesCache(){var search=mock(DiscoverySearchPort.class);var cache=mock(DiscoveryCachePort.class);var service=new DiscoveryProjectionService(search,cache);service.interaction("interaction.like.added","content:42");service.interaction("interaction.post.shared","content:42");verify(search).adjustTrend("content:42",2);verify(search).adjustTrend("content:42",4);verify(cache,times(2)).invalidate();}
 @Test void ignoresEventsThatDoNotAffectRanking(){var search=mock(DiscoverySearchPort.class);var cache=mock(DiscoveryCachePort.class);new DiscoveryProjectionService(search,cache).interaction("interaction.comment.updated","content:42");verifyNoInteractions(search,cache);}
}
