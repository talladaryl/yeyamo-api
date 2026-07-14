package com.yeyamo_mobile.api.notification_service.application;
import java.util.Map;import java.util.UUID;
public record NotificationIntent(UUID sourceEventId,String eventType,String recipientId,String email,Map<String,String>variables,String dataJson){}
