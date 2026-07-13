package com.yeyamo_mobile.api.catalog_service.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.catalog_service.application.CatalogAssetService;

class IngestionEventConsumerTests {
    @Test void processesAnIngestionEventOnlyOnce() throws Exception{
        CatalogAssetService service=mock(CatalogAssetService.class);ProcessedEventRepository receipts=mock(ProcessedEventRepository.class);
        UUID eventId=UUID.randomUUID();when(receipts.existsById(eventId)).thenReturn(false,true);
        IngestionEventConsumer consumer=new IngestionEventConsumer(new ObjectMapper(),service,receipts);
        String event="""
          {"eventId":"%s","eventType":"catalog.asset.ingested","eventVersion":1,"correlationId":"job-1",
           "payload":{"source":"partner-api","externalId":"P-42","assetType":"PLACE","name":"Museum",
           "categoryCode":"CULTURE","regionCode":"CM-CE","city":"Yaounde","latitude":3.87,"longitude":11.52}}
          """.formatted(eventId);
        consumer.consume(event);consumer.consume(event);
        verify(service,times(1)).synchronizeExternalAsset(eq("partner-api"),eq("P-42"),any(),isNull(),eq("Museum"),isNull(),
                isNull(),eq("CULTURE"),eq("CM-CE"),eq("Yaounde"),isNull(),isNull(),eq(3.87),eq(11.52),any(),eq("job-1"),eq("ingestion-service"));
        verify(receipts,times(1)).save(any(ProcessedEventEntity.class));
    }
}
