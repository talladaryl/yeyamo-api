package com.yeyamo_mobile.api.catalog_service.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.*;import static org.mockito.Mockito.*;
import java.util.UUID;import org.junit.jupiter.api.Test;import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.catalog_service.application.CatalogAssetService;import com.yeyamo_mobile.api.catalog_service.domain.model.AssetStatus;

class LegacyPlaceEventConsumerTests {
 @Test void mapsVersionTwoPlaceEventsToCatalogAssets() throws Exception{
  CatalogAssetService service=mock(CatalogAssetService.class);ProcessedEventRepository processed=mock(ProcessedEventRepository.class);UUID eventId=UUID.randomUUID(),placeId=UUID.randomUUID(),partnerId=UUID.randomUUID();
  String raw="{\"eventId\":\""+eventId+"\",\"eventType\":\"place.updated\",\"eventVersion\":2,\"correlationId\":\"corr-1\",\"payload\":{\"placeId\":\""+placeId+"\",\"partnerId\":\""+partnerId+"\",\"name\":\"Musee\",\"slug\":\"musee\",\"description\":\"Culture\",\"category\":\"museum\",\"regionCode\":\"LT\",\"city\":\"Douala\",\"district\":\"Akwa\",\"address\":\"Centre\",\"latitude\":4.05,\"longitude\":9.70,\"status\":\"PUBLISHED\"}}";
  new LegacyPlaceEventConsumer(new ObjectMapper(),service,processed).consume(raw);
  verify(service).synchronizeLegacyPlace(eq(placeId.toString()),eq(partnerId),eq("Musee"),eq("musee"),eq("Culture"),eq("museum"),eq("LT"),eq("Douala"),eq("Akwa"),eq("Centre"),eq(4.05),eq(9.70),eq(AssetStatus.PUBLISHED),eq("corr-1"));
  verify(processed).save(argThat(done->done.getEventId().equals(eventId)));
 }
 @Test void ignoresAnAlreadyProcessedLegacyEvent() throws Exception{
  CatalogAssetService service=mock(CatalogAssetService.class);ProcessedEventRepository processed=mock(ProcessedEventRepository.class);UUID eventId=UUID.randomUUID();when(processed.existsById(eventId)).thenReturn(true);
  String raw="{\"eventId\":\""+eventId+"\",\"eventType\":\"place.updated\",\"eventVersion\":2,\"payload\":{}}";
  new LegacyPlaceEventConsumer(new ObjectMapper(),service,processed).consume(raw);
  verifyNoInteractions(service);verify(processed,never()).save(any());
 }
}
