package com.yeyamo_mobile.api.media_service.infrastructure.outbox;
import java.time.Instant;import java.util.*;import org.springframework.stereotype.Component;import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.media_service.application.port.MediaOutboxPort;import com.yeyamo_mobile.api.media_service.domain.model.MediaAsset;
@Component
public class JpaMediaOutboxAdapter implements MediaOutboxPort{
 private final MediaOutboxRepository repo;private final ObjectMapper mapper;public JpaMediaOutboxAdapter(MediaOutboxRepository r,ObjectMapper m){repo=r;mapper=m;}
 public void append(String eventType,MediaAsset media,String correlationId,String actorId){try{UUID id=UUID.randomUUID();Map<String,Object> payload=new LinkedHashMap<>();
  payload.put("mediaId",media.getId());payload.put("ownerId",media.getOwnerId());payload.put("type",media.getType());payload.put("status",media.getStatus());
  payload.put("thumbnailStatus",media.getThumbnailStatus());payload.put("contentType",media.getContentType());payload.put("sizeBytes",media.getSizeBytes());payload.put("checksum",media.getChecksum());
  payload.put("aggregateType",media.getAggregateType());payload.put("aggregateId",media.getAggregateId());payload.put("width",media.getWidth());payload.put("height",media.getHeight());payload.put("durationMs",media.getDurationMs());
  Map<String,Object> event=new LinkedHashMap<>();event.put("eventId",id);event.put("eventType",eventType);event.put("eventVersion",1);event.put("occurredAt",Instant.now());event.put("producer","media-service");
  event.put("aggregateType","media");event.put("aggregateId",media.getId().toString());event.put("correlationId",correlationId==null?id.toString():correlationId);event.put("actorId",actorId);event.put("payload",payload);
  MediaOutboxEvent e=new MediaOutboxEvent();e.setId(id);e.setAggregateId(media.getId().toString());e.setEventType(eventType);e.setOccurredAt(Instant.now());e.setPayload(mapper.writeValueAsString(event));repo.save(e);
 }catch(Exception e){throw new IllegalStateException("Cannot create media outbox event",e);}}
}
