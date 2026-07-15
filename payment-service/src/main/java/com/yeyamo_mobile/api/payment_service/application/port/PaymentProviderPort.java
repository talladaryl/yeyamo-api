package com.yeyamo_mobile.api.payment_service.application.port;
import java.math.BigDecimal;import java.util.UUID;
public interface PaymentProviderPort{
 enum Outcome{SUCCEEDED,PENDING,FAILED}
 record ProviderResult(Outcome outcome,String providerOperationId,String reason){}
 record AuthorizationRequest(UUID paymentId,UUID bookingId,String userId,BigDecimal amount,String currency,String idempotencyKey){}
 record RefundRequest(UUID refundId,String providerPaymentId,BigDecimal amount,String currency,String idempotencyKey){}
 String name();ProviderResult authorize(AuthorizationRequest request);ProviderResult cancel(String providerPaymentId,String idempotencyKey);ProviderResult refund(RefundRequest request);
}
