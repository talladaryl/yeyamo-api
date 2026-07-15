package com.yeyamo_mobile.api.payment_service.application;
import java.math.BigDecimal;import java.util.UUID;
public final class PaymentCommands{private PaymentCommands(){}
 public record Authorize(UUID sagaId,UUID bookingId,String userId,BigDecimal amount,String currency,String idempotencyKey,String correlationId){}
 public record CancelAuthorization(UUID sagaId,UUID bookingId,String idempotencyKey,String correlationId){}
 public record Refund(UUID sagaId,UUID bookingId,String paymentId,BigDecimal amount,String currency,String idempotencyKey,String correlationId){}
}
