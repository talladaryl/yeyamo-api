package com.yeyamo_mobile.api.catalog_service.infrastructure.outbox;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.catalog_service.application.port.CatalogReferenceOutboxPort;
import com.yeyamo_mobile.api.catalog_service.domain.model.CatalogReference;

@Component
public class JpaCatalogReferenceOutboxAdapter implements CatalogReferenceOutboxPort {
    private final CatalogOutboxRepository repository; private final ObjectMapper mapper;
    public JpaCatalogReferenceOutboxAdapter(CatalogOutboxRepository repository,ObjectMapper mapper){this.repository=repository;this.mapper=mapper;}
    public void append(String eventType,CatalogReference reference,String correlationId,String actorId){
        try{
            UUID eventId=UUID.randomUUID();Map<String,Object> payload=new LinkedHashMap<>();
            payload.put("referenceId",reference.getId());payload.put("type",reference.getType());payload.put("code",reference.getCode());
            payload.put("name",reference.getName());payload.put("parentCode",reference.getParentCode());payload.put("countryCode",reference.getCountryCode());payload.put("active",reference.isActive());
            Map<String,Object> envelope=new LinkedHashMap<>();envelope.put("eventId",eventId);envelope.put("eventType",eventType);envelope.put("eventVersion",1);envelope.put("occurredAt",Instant.now());
            envelope.put("producer","catalog-service");envelope.put("aggregateType","catalog-reference");envelope.put("aggregateId",reference.getId().toString());
            envelope.put("correlationId",normalize(correlationId,eventId.toString()));envelope.put("actorId",normalize(actorId,"system"));envelope.put("payload",payload);
            CatalogOutboxEvent event=new CatalogOutboxEvent();event.setId(eventId);event.setAggregateId(reference.getId().toString());event.setEventType(eventType);event.setOccurredAt(Instant.now());event.setPayload(mapper.writeValueAsString(envelope));repository.save(event);
        }catch(Exception exception){throw new IllegalStateException("Cannot serialize catalog reference event",exception);}
    }
    private String normalize(String value,String fallback){return value==null||value.isBlank()?fallback:value;}
}
