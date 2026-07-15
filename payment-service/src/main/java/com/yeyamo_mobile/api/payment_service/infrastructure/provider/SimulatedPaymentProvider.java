package com.yeyamo_mobile.api.payment_service.infrastructure.provider;
import java.math.BigDecimal;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;import com.yeyamo_mobile.api.payment_service.application.port.PaymentProviderPort;
@Component public class SimulatedPaymentProvider implements PaymentProviderPort{
 private final String name;private final BigDecimal declineThreshold;
 public SimulatedPaymentProvider(@Value("${payment.provider.name:simulated}")String name,@Value("${payment.provider.simulated.decline-threshold:1000000.00}")BigDecimal threshold){this.name=name;declineThreshold=threshold;}
 public String name(){return name;}
 public ProviderResult authorize(AuthorizationRequest request){if(request.amount().compareTo(declineThreshold)>=0)return new ProviderResult(Outcome.FAILED,null,"Amount rejected by provider policy");return new ProviderResult(Outcome.SUCCEEDED,"pay_"+request.paymentId(),null);}
 public ProviderResult cancel(String providerPaymentId,String key){return new ProviderResult(Outcome.SUCCEEDED,providerPaymentId,null);}
 public ProviderResult refund(RefundRequest request){return new ProviderResult(Outcome.SUCCEEDED,"refund_"+request.refundId(),null);}
}
