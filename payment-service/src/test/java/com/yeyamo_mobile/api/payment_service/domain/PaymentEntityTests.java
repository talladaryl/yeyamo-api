package com.yeyamo_mobile.api.payment_service.domain;
import static org.junit.jupiter.api.Assertions.*;import java.math.BigDecimal;import java.util.UUID;import org.junit.jupiter.api.Test;import com.yeyamo_mobile.api.payment_service.infrastructure.persistence.*;
class PaymentEntityTests{
 @Test void authorizationThenRefundFollowsTheExpectedWorkflow(){var payment=PaymentEntity.pending(UUID.randomUUID(),UUID.randomUUID(),"user-1",new BigDecimal("25.00"),"eur","simulated","key-1");payment.authorized("pay-1");assertEquals(PaymentStatus.AUTHORIZED,payment.getStatus());var refund=RefundEntity.pending(payment,new BigDecimal("25.00"),"refund-key");payment.refundPending();refund.succeeded("refund-1");payment.refunded();assertEquals(PaymentStatus.REFUNDED,payment.getStatus());assertEquals(RefundStatus.SUCCEEDED,refund.getStatus());}
 @Test void rejectsRefundAboveCapturedAmount(){var payment=PaymentEntity.pending(UUID.randomUUID(),null,"user-1",BigDecimal.TEN,"EUR","simulated","key");payment.authorized("pay");assertThrows(PaymentException.class,()->RefundEntity.pending(payment,new BigDecimal("11.00"),"refund"));}
}
