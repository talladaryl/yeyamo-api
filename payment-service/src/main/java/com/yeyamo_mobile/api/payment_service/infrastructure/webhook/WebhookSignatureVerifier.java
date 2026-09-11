package com.yeyamo_mobile.api.payment_service.infrastructure.webhook;
import java.nio.charset.StandardCharsets;import java.security.*;import java.time.Instant;import java.util.HexFormat;import javax.crypto.Mac;import javax.crypto.spec.SecretKeySpec;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;import com.yeyamo_mobile.api.payment_service.domain.PaymentException;
@Component public class WebhookSignatureVerifier{
 private final byte[] secret;private final long tolerance;
 public WebhookSignatureVerifier(@Value("${payment.webhook.secret}")String secret,@Value("${payment.webhook.tolerance-seconds:300}")long tolerance){if(secret==null||secret.length()<24)throw new IllegalArgumentException("PAYMENT_AGGREGATOR_WEBHOOK_SECRET must contain at least 24 characters");this.secret=secret.getBytes(StandardCharsets.UTF_8);this.tolerance=tolerance;}
 public void verify(String timestamp,String signature,String payload){try{long epoch=Long.parseLong(timestamp);if(Math.abs(Instant.now().getEpochSecond()-epoch)>tolerance)throw invalid();Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));byte[] expected=mac.doFinal((timestamp+"."+payload).getBytes(StandardCharsets.UTF_8));byte[] actual=HexFormat.of().parseHex(signature);if(!MessageDigest.isEqual(expected,actual))throw invalid();}catch(PaymentException e){throw e;}catch(Exception e){throw invalid();}}
 private PaymentException invalid(){return new PaymentException("INVALID_WEBHOOK_SIGNATURE","Invalid or expired webhook signature");}
}
