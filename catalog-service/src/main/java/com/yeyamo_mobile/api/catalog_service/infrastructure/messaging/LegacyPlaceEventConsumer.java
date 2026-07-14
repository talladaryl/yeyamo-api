package com.yeyamo_mobile.api.catalog_service.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.catalog_service.application.CatalogAssetService;
import com.yeyamo_mobile.api.catalog_service.domain.model.AssetStatus;

@Component
public class LegacyPlaceEventConsumer {
    private final ObjectMapper mapper; private final CatalogAssetService service;
    private final ProcessedEventRepository processed;
    public LegacyPlaceEventConsumer(ObjectMapper mapper,CatalogAssetService service,ProcessedEventRepository processed){
        this.mapper=mapper;this.service=service;this.processed=processed;
    }
    @KafkaListener(topics="${yeyamo.kafka.topics.place-events:place.events}",
            groupId="${spring.kafka.consumer.group-id:catalog-service}")
    @Transactional
    public void consume(String raw) throws Exception {
        JsonNode event=mapper.readTree(raw);
        UUID eventId=UUID.fromString(required(event,"eventId"));
        if(processed.existsById(eventId))return;
        String eventType=required(event,"eventType");
        int version=event.path("eventVersion").asInt(0);
        if(!eventType.startsWith("place.")||version<1||version>2)
            throw new IllegalArgumentException("Unsupported place event contract");
        JsonNode p=event.path("payload");
        String placeId=required(p,"placeId");
        service.synchronizeLegacyPlace(placeId,uuid(p,"partnerId"),required(p,"name"),
                text(p,"slug",null),text(p,"description",null),text(p,"category",null),
                text(p,"regionCode",null),text(p,"city",null),text(p,"district",null),
                text(p,"address",null),p.path("latitude").asDouble(),p.path("longitude").asDouble(),
                status(text(p,"status","DRAFT")),text(event,"correlationId",eventId.toString()));
        ProcessedEventEntity done=new ProcessedEventEntity();done.setEventId(eventId);
        done.setEventType(eventType);done.setProcessedAt(Instant.now());processed.save(done);
    }
    private String required(JsonNode n,String f){String v=text(n,f,null);if(v==null)throw new IllegalArgumentException(f+" is required");return v;}
    private String text(JsonNode n,String f,String d){JsonNode v=n.get(f);return v==null||v.isNull()?d:v.asText();}
    private UUID uuid(JsonNode n,String f){String v=text(n,f,null);return v==null?null:UUID.fromString(v);}
    private AssetStatus status(String v){return switch(v){case "PUBLISHED"->AssetStatus.PUBLISHED;case "ARCHIVED"->AssetStatus.ARCHIVED;default->AssetStatus.DRAFT;};}
}
