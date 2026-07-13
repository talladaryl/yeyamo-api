package com.yeyamo_mobile.api.interaction_service.domain.model;
import java.time.Instant;import java.util.UUID;
public record CommandReceipt(UUID id,String idempotencyKey,String actorId,String operation,UUID resultId,boolean changed,Instant createdAt){
 public static CommandReceipt create(String key,String actor,String operation,UUID result,boolean changed){if(key==null||key.isBlank())throw new IllegalArgumentException("Idempotency-Key is required");if(key.trim().length()>200)throw new IllegalArgumentException("Idempotency-Key exceeds 200 characters");if(actor==null||actor.isBlank())throw new IllegalArgumentException("actor is required");if(operation==null||operation.isBlank()||operation.length()>200)throw new IllegalArgumentException("operation is invalid");if(result==null)throw new IllegalArgumentException("result is required");
  return new CommandReceipt(UUID.randomUUID(),key.trim(),actor,operation,result,changed,Instant.now());}
}
