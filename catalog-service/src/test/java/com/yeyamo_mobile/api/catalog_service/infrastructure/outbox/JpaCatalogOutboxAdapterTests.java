package com.yeyamo_mobile.api.catalog_service.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
import java.util.*;import org.junit.jupiter.api.Test;import org.mockito.ArgumentCaptor;
import com.fasterxml.jackson.databind.ObjectMapper;import com.yeyamo_mobile.api.catalog_service.domain.model.*;

class JpaCatalogOutboxAdapterTests {
    @Test void writesTheVersionedCatalogEventEnvelope() throws Exception{
        CatalogOutboxRepository repository=mock(CatalogOutboxRepository.class);ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();
        CatalogAsset asset=CatalogAsset.create(AssetType.DESTINATION,null,"catalog",null,"Cameroon","cameroon",null,null,null,null,null,null,new GeoPoint(4,12));
        new JpaCatalogOutboxAdapter(repository,mapper).append("catalog.asset.created",asset,"corr-1","admin-1");
        ArgumentCaptor<CatalogOutboxEvent> event=ArgumentCaptor.forClass(CatalogOutboxEvent.class);verify(repository).save(event.capture());
        var json=mapper.readTree(event.getValue().getPayload());assertEquals(1,json.get("eventVersion").asInt());assertEquals("catalog-service",json.get("producer").asText());assertEquals("corr-1",json.get("correlationId").asText());
    }
}
