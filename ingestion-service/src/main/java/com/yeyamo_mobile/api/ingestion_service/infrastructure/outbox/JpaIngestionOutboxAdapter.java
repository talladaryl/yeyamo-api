package com.yeyamo_mobile.api.ingestion_service.infrastructure.outbox;
import java.time.Instant;import java.util.*;import org.springframework.stereotype.Component;import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.ingestion_service.application.port.IngestionOutboxPort;import com.yeyamo_mobile.api.ingestion_service.domain.model.NormalizedCatalogRecord;
@Component
public class JpaIngestionOutboxAdapter implements IngestionOutboxPort{
 private final IngestionOutboxRepository repo;private final ObjectMapper mapper;public JpaIngestionOutboxAdapter(IngestionOutboxRepository r,ObjectMapper m){repo=r;mapper=m;}
 public void append(UUID jobId,NormalizedCatalogRecord record){try{UUID eventId=UUID.randomUUID();Map<String,Object> envelope=new LinkedHashMap<>();
  envelope.put("eventId",eventId);envelope.put("eventType","catalog.asset.ingested");envelope.put("eventVersion",1);envelope.put("occurredAt",Instant.now());
  envelope.put("producer","ingestion-service");envelope.put("aggregateType","catalog-asset-candidate");envelope.put("aggregateId",record.fingerprint());
  envelope.put("correlationId",jobId.toString());envelope.put("actorId","ingestion-worker");envelope.put("payload",record);
  IngestionOutboxEvent e=new IngestionOutboxEvent();e.setId(eventId);e.setAggregateId(record.fingerprint());e.setEventType("catalog.asset.ingested");
  e.setOccurredAt(Instant.now());e.setPayload(mapper.writeValueAsString(envelope));repo.save(e);
 }catch(Exception ex){throw new IllegalStateException("Cannot create ingestion outbox event",ex);}}
}
