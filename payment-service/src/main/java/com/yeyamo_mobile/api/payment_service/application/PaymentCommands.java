package com.yeyamo_mobile.api.payment_service.application;
import java.math.BigDecimal;import java.util.UUID;import com.yeyamo_mobile.api.payment_service.domain.PaymentSourceType;
public final class PaymentCommands{private PaymentCommands(){}
 public record Authorize(UUID sagaId,PaymentSourceType sourceType,UUID sourceId,String userId,String partnerId,BigDecimal amount,String currency,String idempotencyKey,String correlationId){public Authorize(UUID sagaId,UUID sourceId,String userId,BigDecimal amount,String currency,String idempotencyKey,String correlationId){this(sagaId,PaymentSourceType.BOOKING,sourceId,userId,null,amount,currency,idempotencyKey,correlationId);}}
 public record CancelAuthorization(UUID sagaId,PaymentSourceType sourceType,UUID sourceId,String idempotencyKey,String correlationId){public CancelAuthorization(UUID sagaId,UUID sourceId,String key,String correlation){this(sagaId,PaymentSourceType.BOOKING,sourceId,key,correlation);}}
 public record Refund(UUID sagaId,PaymentSourceType sourceType,UUID sourceId,String paymentId,BigDecimal amount,String currency,String idempotencyKey,String correlationId){public Refund(UUID sagaId,UUID sourceId,String paymentId,BigDecimal amount,String currency,String key,String correlation){this(sagaId,PaymentSourceType.BOOKING,sourceId,paymentId,amount,currency,key,correlation);}}
}
