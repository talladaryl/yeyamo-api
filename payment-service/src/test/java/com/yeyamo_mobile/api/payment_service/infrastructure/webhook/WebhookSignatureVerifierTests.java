package com.yeyamo_mobile.api.payment_service.infrastructure.webhook;
import static org.junit.jupiter.api.Assertions.*;import java.nio.charset.StandardCharsets;import java.time.Instant;import java.util.HexFormat;import javax.crypto.Mac;import javax.crypto.spec.SecretKeySpec;import org.junit.jupiter.api.Test;import com.yeyamo_mobile.api.payment_service.domain.PaymentException;
class WebhookSignatureVerifierTests{
 static final String SECRET="01234567890123456789012345678901";
 @Test void acceptsCurrentValidSignature()throws Exception{String timestamp=Long.toString(Instant.now().getEpochSecond()),payload="{\"eventId\":\"evt-1\"}";new WebhookSignatureVerifier(SECRET,300).verify(timestamp,sign(timestamp,payload),payload);}
 @Test void rejectsInvalidSignature(){String timestamp=Long.toString(Instant.now().getEpochSecond());assertThrows(PaymentException.class,()->new WebhookSignatureVerifier(SECRET,300).verify(timestamp,"00","{}"));}
 private String sign(String t,String p)throws Exception{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal((t+"."+p).getBytes(StandardCharsets.UTF_8)));}
}
