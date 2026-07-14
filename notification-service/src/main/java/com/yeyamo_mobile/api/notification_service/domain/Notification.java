package com.yeyamo_mobile.api.notification_service.domain;
import java.time.Instant;import java.util.UUID;
public record Notification(UUID id,UUID sourceEventId,String eventType,String recipientId,String title,String body,String dataJson,Instant createdAt,Instant readAt){
 public Notification{if(id==null||sourceEventId==null)throw new IllegalArgumentException("notification identifiers are required");if(eventType==null||eventType.isBlank())throw new IllegalArgumentException("eventType is required");if(recipientId==null||recipientId.isBlank())throw new IllegalArgumentException("recipientId is required");if(title==null||title.isBlank()||body==null||body.isBlank())throw new IllegalArgumentException("notification content is required");}
 public Notification read(Instant now){return readAt==null?new Notification(id,sourceEventId,eventType,recipientId,title,body,dataJson,createdAt,now):this;}
}
