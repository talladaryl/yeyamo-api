package com.yeyamo_mobile.api.interaction_service.infrastructure.outbox;
import java.time.Instant;import java.util.*;import org.springframework.stereotype.Component;import com.fasterxml.jackson.databind.ObjectMapper;import com.yeyamo_mobile.api.interaction_service.application.port.InteractionOutboxPort;
@Component
public class JpaInteractionOutboxAdapter implements InteractionOutboxPort{
 private final InteractionOutboxRepository repo;private final ObjectMapper mapper;public JpaInteractionOutboxAdapter(InteractionOutboxRepository r,ObjectMapper m){repo=r;mapper=m;}
 public void append(String eventType,String aggregateType,String aggregateId,String actorId,String correlationId,Map<String,Object> payload){try{UUID id=UUID.randomUUID();Map<String,Object> event=new LinkedHashMap<>();
  event.put("eventId",id);event.put("eventType",eventType);event.put("eventVersion",1);event.put("occurredAt",Instant.now());event.put("producer","interaction-service");event.put("aggregateType",aggregateType);
  event.put("aggregateId",aggregateId);event.put("correlationId",correlationId==null?id.toString():correlationId);event.put("actorId",actorId);event.put("payload",payload);
  InteractionOutboxEvent e=new InteractionOutboxEvent();e.setId(id);e.setAggregateId(aggregateId);e.setEventType(eventType);e.setOccurredAt(Instant.now());e.setPayload(mapper.writeValueAsString(event));repo.save(e);
 }catch(Exception e){throw new IllegalStateException("Cannot create interaction outbox event",e);}}
}
