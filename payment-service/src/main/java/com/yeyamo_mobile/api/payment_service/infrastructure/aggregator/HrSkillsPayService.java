package com.yeyamo_mobile.api.payment_service.infrastructure.aggregator;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.payment_service.application.port.PaymentOutboxPort;
import com.yeyamo_mobile.api.payment_service.domain.PaymentAttemptStatus;
import com.yeyamo_mobile.api.payment_service.domain.PaymentException;
import com.yeyamo_mobile.api.payment_service.infrastructure.persistence.PaymentAttemptEntity;
import com.yeyamo_mobile.api.payment_service.infrastructure.persistence.PaymentAttemptRepository;

@Service
public class HrSkillsPayService {

    private static final Logger log = LoggerFactory.getLogger(HrSkillsPayService.class);

    private final HrSkillsPayClient client;
    private final PaymentAttemptRepository attemptRepository;
    private final HrSkillsPayProperties properties;
    private final ObjectMapper objectMapper;
    private final PaymentOutboxPort outbox;

    @Autowired
    public HrSkillsPayService(
            HrSkillsPayClient client,
            PaymentAttemptRepository attemptRepository,
            HrSkillsPayProperties properties,
            ObjectMapper objectMapper,
            PaymentOutboxPort outbox) {
        this.client = client;
        this.attemptRepository = attemptRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.outbox = outbox;
    }

    public boolean isEnabled() {
        return properties.getKeyA() != null && !properties.getKeyA().isBlank()
                && properties.getKeyB() != null && !properties.getKeyB().isBlank();
    }

    @Transactional
    public PaymentAttemptEntity handleAuthorizationCommand(JsonNode event, JsonNode payload, String correlationId) {
        UUID sourceId = UUID.fromString(required(payload, "bookingId"));
        String idempotencyKey = required(payload, "idempotencyKey");
        BigDecimal amount = decimal(payload, "amount");
        String currency = required(payload, "currency");

        String sourceService = idempotencyKey.startsWith("ticket:") ? "ticket" : "booking";

        // Détection des champs spécifiques HR-Skills Pay (operator, country, phone_number)
        String operator = text(payload, "operator");
        String country = text(payload, "country");
        String phoneNumber = text(payload, "phone_number");
        if (phoneNumber == null) {
            phoneNumber = text(payload, "phoneNumber");
        }

        // Si des champs requis sont absents du contrat producteur
        if (operator == null || country == null || phoneNumber == null) {
            String gapMessage = String.format(
                    "CONTRACT_GAP_MISSING_PAYMENT_DETAILS: producer '%s' payload does not provide required cash-in fields (operator=%s, country=%s, phone_number=%s)",
                    sourceService, operator, country, phoneNumber);
            log.warn(gapMessage);

            // Publication immédiate de l'échec vers le topic de retour
            publishPaymentFailed(sourceId, correlationId, gapMessage);
            return null;
        }

        // Vérification de réutilisation de clé d'idempotence pour un même paiement logique
        Optional<PaymentAttemptEntity> existingOpt = attemptRepository.findByIdempotencyKey(idempotencyKey);
        String cashInKey = existingOpt.map(PaymentAttemptEntity::getIdempotencyKey).orElse(idempotencyKey);

        CashInResponse response;
        try {
            CashInRequest request = new CashInRequest(operator, country, phoneNumber, amount, currency, cashInKey);
            response = client.initiateCashIn(request);
        } catch (Exception ex) {
            log.error("Immediate failure during HR-Skills Pay Cash-In initiation: {}", ex.getMessage());
            publishPaymentFailed(sourceId, correlationId, ex.getMessage());
            return null;
        }

        PaymentAttemptEntity attempt = existingOpt.orElseGet(() ->
                PaymentAttemptEntity.initiated(
                        response.reference(), response.transactionId(), sourceService, sourceId,
                        amount, currency, idempotencyKey));
        return attemptRepository.save(attempt);
    }

    @Transactional
    public void processWebhook(byte[] rawBody, String signatureHeader) throws Exception {
        verifyHmacSignature(rawBody, signatureHeader);

        JsonNode root = objectMapper.readTree(rawBody);
        String reference = text(root, "reference");
        if (reference == null || reference.isBlank()) {
            throw new PaymentException("INVALID_WEBHOOK_PAYLOAD", "Missing reference in webhook payload");
        }

        PaymentAttemptEntity attempt = attemptRepository.findByReference(reference)
                .orElseThrow(() -> new PaymentException("PAYMENT_NOT_FOUND", "No payment attempt found for reference " + reference));

        // Règle d'idempotence stricte : ne pas republier si déjà traité
        if (attempt.getStatus() == PaymentAttemptStatus.SUCCESS || attempt.getStatus() == PaymentAttemptStatus.FAILED) {
            log.info("Ignoring duplicate webhook for reference {} already in state {}", reference, attempt.getStatus());
            return;
        }

        String eventType = text(root, "event");
        if (eventType == null) {
            eventType = text(root, "type");
        }
        String status = text(root, "status");

        if ("payment.succeeded".equalsIgnoreCase(eventType) || "SUCCESS".equalsIgnoreCase(status)) {
            attempt.markSuccess();
            attemptRepository.save(attempt);
            publishPaymentAuthorized(attempt.getSourceId(), reference, attempt.getAmount(), attempt.getCurrency());
        } else if ("payment.failed".equalsIgnoreCase(eventType) || "FAILED".equalsIgnoreCase(status)) {
            String failureReason = text(root, "message");
            if (failureReason == null) {
                failureReason = text(root, "reason");
            }
            if (failureReason == null) {
                failureReason = "Payment rejected by aggregator";
            }
            attempt.markFailed(failureReason);
            attemptRepository.save(attempt);
            publishPaymentFailed(attempt.getSourceId(), null, failureReason);
        } else if ("payment.hold".equalsIgnoreCase(eventType) || "HOLD".equalsIgnoreCase(status)) {
            // Traitement AML à part : log d'alerte sans validation/rejet automatique
            String holdReason = text(root, "message");
            if (holdReason == null) holdReason = "Transaction flagged by AML compliance";
            attempt.markHold(holdReason);
            attemptRepository.save(attempt);
            log.error("AML ALERT - TRANSACTION HOLD: reference={}, sourceService={}, sourceId={}, amount={} {}, reason={}. Manual investigation required.",
                    reference, attempt.getSourceService(), attempt.getSourceId(), attempt.getAmount(), attempt.getCurrency(), holdReason);
        }
    }

    public void verifyHmacSignature(byte[] rawBody, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new PaymentException("INVALID_WEBHOOK_SIGNATURE", "Missing X-Hub-Signature header");
        }

        String secret = properties.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new PaymentException("INVALID_WEBHOOK_SIGNATURE", "Webhook secret is not configured");
        }

        String providedHex = signatureHeader.trim();
        if (providedHex.startsWith("sha256=")) {
            providedHex = providedHex.substring(7);
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] expectedHmac = mac.doFinal(rawBody);
            byte[] actualHmac = HexFormat.of().parseHex(providedHex);

            // Comparaison en temps constant obligatoire
            if (!MessageDigest.isEqual(expectedHmac, actualHmac)) {
                throw new PaymentException("INVALID_WEBHOOK_SIGNATURE", "HMAC signature mismatch");
            }
        } catch (PaymentException pe) {
            throw pe;
        } catch (Exception ex) {
            throw new PaymentException("INVALID_WEBHOOK_SIGNATURE", "Error verifying webhook signature: " + ex.getMessage());
        }
    }

    private void publishPaymentAuthorized(UUID sourceId, String reference, BigDecimal amount, String currency) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookingId", sourceId.toString());
        payload.put("paymentId", reference);
        payload.put("amount", amount);
        payload.put("currency", currency);
        outbox.append("payment.authorized", sourceId.toString(), UUID.randomUUID().toString(), payload);
    }

    private void publishPaymentFailed(UUID sourceId, String correlationId, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookingId", sourceId.toString());
        payload.put("reason", reason != null ? reason : "Payment failed");
        outbox.append("payment.failed", sourceId.toString(),
                correlationId != null ? correlationId : UUID.randomUUID().toString(), payload);
    }

    private String required(JsonNode n, String f) {
        String v = text(n, f);
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " is required");
        return v;
    }

    private BigDecimal decimal(JsonNode n, String f) {
        String v = required(n, f);
        try {
            return new BigDecimal(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(f + " is invalid");
        }
    }

    private String text(JsonNode n, String f) {
        JsonNode v = n.get(f);
        return v == null || v.isNull() ? null : v.asText();
    }
}
