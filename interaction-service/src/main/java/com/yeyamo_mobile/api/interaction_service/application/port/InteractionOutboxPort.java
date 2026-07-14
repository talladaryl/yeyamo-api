package com.yeyamo_mobile.api.interaction_service.application.port;
import java.util.Map;
public interface InteractionOutboxPort{void append(String eventType,String aggregateType,String aggregateId,String actorId,String correlationId,Map<String,Object> payload);}
