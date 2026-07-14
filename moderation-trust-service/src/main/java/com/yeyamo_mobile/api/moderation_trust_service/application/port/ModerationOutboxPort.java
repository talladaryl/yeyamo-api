package com.yeyamo_mobile.api.moderation_trust_service.application.port;import java.util.Map;
public interface ModerationOutboxPort{void append(String type,String aggregateType,String aggregateId,String actor,String correlation,Map<String,Object>payload);}
