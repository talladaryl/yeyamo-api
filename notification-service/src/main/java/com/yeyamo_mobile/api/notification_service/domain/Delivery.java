package com.yeyamo_mobile.api.notification_service.domain;
import java.time.Instant;import java.util.UUID;
public record Delivery(UUID id,UUID notificationId,NotificationChannel channel,String destination,DeliveryStatus status,int attempts,Instant nextAttemptAt,Instant deliveredAt,String lastError){
 public Delivery sent(Instant now){return new Delivery(id,notificationId,channel,destination,DeliveryStatus.SENT,attempts+1,null,now,null);}
 public Delivery failed(String error,Instant now,int maxAttempts,long baseDelaySeconds){int next=attempts+1;if(next>=maxAttempts)return new Delivery(id,notificationId,channel,destination,DeliveryStatus.DEAD_LETTER,next,null,null,shortError(error));long delay=Math.min(3600,baseDelaySeconds*(1L<<Math.min(10,next-1)));return new Delivery(id,notificationId,channel,destination,DeliveryStatus.RETRY,next,now.plusSeconds(delay),null,shortError(error));}
 private String shortError(String value){if(value==null)return "Delivery failed";return value.length()>1000?value.substring(0,1000):value;}
}
