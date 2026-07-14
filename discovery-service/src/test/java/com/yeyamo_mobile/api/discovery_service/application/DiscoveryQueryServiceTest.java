package com.yeyamo_mobile.api.discovery_service.application;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import java.time.Instant;import java.util.*;import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.discovery_service.application.port.*;import com.yeyamo_mobile.api.discovery_service.domain.model.*;
class DiscoveryQueryServiceTest {
 @Test void usesCacheWithoutSearching(){var port=mock(DiscoverySearchPort.class);var cache=mock(DiscoveryCachePort.class);var criteria=criteria();var cached=new DiscoveryPage(0,2,false,List.of(doc("1")),Instant.now());when(cache.get(criteria)).thenReturn(Optional.of(cached));assertSame(cached,new DiscoveryQueryService(port,cache).search(criteria));verifyNoInteractions(port);}
 @Test void computesHasNextAndCachesPage(){var port=mock(DiscoverySearchPort.class);var cache=mock(DiscoveryCachePort.class);var criteria=criteria();when(cache.get(criteria)).thenReturn(Optional.empty());when(port.search(criteria,3)).thenReturn(List.of(doc("1"),doc("2"),doc("3")));var page=new DiscoveryQueryService(port,cache).search(criteria);assertTrue(page.hasNext());assertEquals(2,page.items().size());verify(cache).put(eq(criteria),same(page));}
 private DiscoverySearch criteria(){return new DiscoverySearch("plage",DiscoveryType.PLACE,null,null,null,null,null,0,2,false);}
 private DiscoveryDocument doc(String id){return new DiscoveryDocument(UUID.randomUUID(),"catalog:"+id,DiscoveryType.PLACE,"Place "+id,null,null,null,null,null,null,null,0,true,Instant.now(),Instant.now());}
}
