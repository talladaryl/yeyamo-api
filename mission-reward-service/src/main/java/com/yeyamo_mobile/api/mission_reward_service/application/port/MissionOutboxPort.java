package com.yeyamo_mobile.api.mission_reward_service.application.port;
import java.util.Map;
public interface MissionOutboxPort{void append(String type,String aggregateId,String correlationId,Map<String,Object>payload);}
