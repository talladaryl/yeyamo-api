package com.yeyamo_mobile.api.partner_service.application.port;
import java.util.Map;import java.util.UUID;
public interface PartnerOutboxPort { void append(String type,UUID partnerId,String actorId,String correlationId,Map<String,Object> payload); }
