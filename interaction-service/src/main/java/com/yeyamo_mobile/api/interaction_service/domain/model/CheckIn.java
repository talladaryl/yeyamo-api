package com.yeyamo_mobile.api.interaction_service.domain.model;
import java.time.Instant;import java.util.UUID;
public record CheckIn(UUID id,UUID catalogAssetId,String userId,Double latitude,Double longitude,boolean visible,Instant occurredAt){
 public static CheckIn create(UUID asset,String user,Double lat,Double lng,boolean visible){if(asset==null)throw new IllegalArgumentException("catalogAssetId is required");if(user==null||user.isBlank())throw new IllegalArgumentException("userId is required");
  if((lat!=null&&(lat<-90||lat>90))||(lng!=null&&(lng<-180||lng>180)))throw new IllegalArgumentException("Invalid check-in coordinates");
  return new CheckIn(UUID.randomUUID(),asset,user,lat,lng,visible,Instant.now());}
}
