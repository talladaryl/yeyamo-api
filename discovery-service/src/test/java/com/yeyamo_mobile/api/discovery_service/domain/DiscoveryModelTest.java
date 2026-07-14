package com.yeyamo_mobile.api.discovery_service.domain;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.discovery_service.application.DiscoverySearch;
import com.yeyamo_mobile.api.discovery_service.domain.model.*;
class DiscoveryModelTest {
 @Test void validatesCoordinates(){assertThrows(IllegalArgumentException.class,()->document(91d,2d));assertThrows(IllegalArgumentException.class,()->document(1d,null));}
 @Test void capsPagination(){var search=new DiscoverySearch(null,null,null,null,null,null,null,-2,500,false);assertEquals(0,search.page());assertEquals(50,search.size());}
 @Test void validatesRadius(){assertThrows(IllegalArgumentException.class,()->new DiscoverySearch(null,null,null,null,1d,2d,201d,0,20,false));}
 private DiscoveryDocument document(Double lat,Double lng){return new DiscoveryDocument(UUID.randomUUID(),"catalog:1",DiscoveryType.PLACE,"Place",null,null,null,null,lat,lng,null,0,true,Instant.now(),Instant.now());}
}
