package com.yeyamo_mobile.api.mission_reward_service.infrastructure.messaging;
import java.util.UUID;import org.springframework.kafka.annotation.KafkaListener;import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.Transactional;import com.fasterxml.jackson.databind.ObjectMapper;import com.yeyamo_mobile.api.mission_reward_service.application.MissionApplicationService;
@Component public class MissionEventConsumer{
 private final ObjectMapper mapper;private final MissionEventMapper eventMapper;private final MissionApplicationService service;private final ProcessedEventRepository processed;
 public MissionEventConsumer(ObjectMapper m,MissionEventMapper e,MissionApplicationService s,ProcessedEventRepository p){mapper=m;eventMapper=e;service=s;processed=p;}
 @KafkaListener(topics={"${yeyamo.kafka.topics.gamification-events:gamification.events}","${yeyamo.kafka.topics.interaction-events:interaction.events}"},groupId="${spring.kafka.consumer.group-id:mission-reward-service}")
 @Transactional public void consume(String raw)throws Exception{var node=mapper.readTree(raw);UUID id=UUID.fromString(required(node,"eventId"));if(processed.existsById(id))return;if(node.path("eventVersion").asInt(0)!=1)throw new IllegalArgumentException("Unsupported event version");String type=required(node,"eventType");service.apply(eventMapper.map(node));processed.save(new ProcessedEventEntity(id,type));}
 private String required(com.fasterxml.jackson.databind.JsonNode n,String f){var v=n.get(f);if(v==null||v.isNull()||v.asText().isBlank())throw new IllegalArgumentException(f+" is required");return v.asText();}
}
