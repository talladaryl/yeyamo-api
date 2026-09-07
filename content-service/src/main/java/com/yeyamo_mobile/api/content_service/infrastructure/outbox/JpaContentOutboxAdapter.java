package com.yeyamo_mobile.api.content_service.infrastructure.outbox;
import java.time.Instant;import java.util.*;import org.springframework.stereotype.Component;import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.content_service.domain.model.Post;
@Component
public class JpaContentOutboxAdapter implements ContentOutboxPort{
 private final ContentOutboxRepository repo;private final ObjectMapper mapper;public JpaContentOutboxAdapter(ContentOutboxRepository r,ObjectMapper m){repo=r;mapper=m;}
 public void append(String eventType,Post post,String correlationId,String actorId){try{UUID id=UUID.randomUUID();Map<String,Object> payload=new LinkedHashMap<>();
  payload.put("postId",post.getId());payload.put("authorId",post.getAuthorId());payload.put("status",post.getStatus());payload.put("visibility",post.getVisibility());payload.put("caption",post.getCaption());
  payload.put("catalogAssetId",post.getCatalogAssetId());payload.put("referenceType",post.getReferenceType());payload.put("referenceId",post.getReferenceId());payload.put("targetType",post.getTargetType());payload.put("targetId",post.getTargetId());payload.put("mediaIds",post.getMediaIds());payload.put("hashtags",post.getHashtags());payload.put("publishedAt",post.getPublishedAt());
  payload.put("countryCode",post.getGeography()==null?null:post.getGeography().getCountryCode());payload.put("languageCode",post.getGeography()==null?null:post.getGeography().getLanguageCode());
  Map<String,Object> event=new LinkedHashMap<>();event.put("eventId",id);event.put("eventType",eventType);event.put("eventVersion",1);event.put("occurredAt",Instant.now());event.put("producer","content-service");
  event.put("aggregateType","post");event.put("aggregateId",post.getId().toString());event.put("correlationId",correlationId==null?id.toString():correlationId);event.put("actorId",actorId);event.put("payload",payload);
  ContentOutboxEvent e=new ContentOutboxEvent();e.setId(id);e.setAggregateId(post.getId().toString());e.setEventType(eventType);e.setOccurredAt(Instant.now());e.setPayload(mapper.writeValueAsString(event));repo.save(e);
 }catch(Exception e){throw new IllegalStateException("Cannot create content outbox event",e);}}
 
 // Méthode générique pour événements non-Post (ex: stories)
 public void append(String eventType,String aggregateId,String actorId,String correlationId,Map<String,String> payloadData){try{UUID id=UUID.randomUUID();
  Map<String,Object> event=new LinkedHashMap<>();event.put("eventId",id);event.put("eventType",eventType);event.put("eventVersion",1);event.put("occurredAt",Instant.now());event.put("producer","content-service");
  event.put("aggregateType","story");event.put("aggregateId",aggregateId);event.put("correlationId",correlationId==null?id.toString():correlationId);event.put("actorId",actorId);event.put("payload",payloadData);
  ContentOutboxEvent e=new ContentOutboxEvent();e.setId(id);e.setAggregateId(aggregateId);e.setEventType(eventType);e.setOccurredAt(Instant.now());e.setPayload(mapper.writeValueAsString(event));repo.save(e);
 }catch(Exception e){throw new IllegalStateException("Cannot create content outbox event",e);}}
}
