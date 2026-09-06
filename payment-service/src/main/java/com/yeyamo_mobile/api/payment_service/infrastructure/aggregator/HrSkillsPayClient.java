package com.yeyamo_mobile.api.payment_service.infrastructure.aggregator;

import java.math.RoundingMode;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.payment_service.domain.PaymentException;

@Component
public class HrSkillsPayClient {

    private static final Logger log = LoggerFactory.getLogger(HrSkillsPayClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final HrSkillsPayTokenManager tokenManager;
    private final HrSkillsPayProperties properties;
    private final ObjectMapper objectMapper;

    @Autowired
    public HrSkillsPayClient(
            @Autowired(required = false) WebClient.Builder webClientBuilder,
            HrSkillsPayTokenManager tokenManager,
            HrSkillsPayProperties properties,
            ObjectMapper objectMapper) {
        this.webClient = webClientBuilder != null ? webClientBuilder.build() : WebClient.builder().build();
        this.tokenManager = tokenManager;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public HrSkillsPayClient(
            WebClient webClient,
            HrSkillsPayTokenManager tokenManager,
            HrSkillsPayProperties properties,
            ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.tokenManager = tokenManager;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public CashInResponse initiateCashIn(CashInRequest request) {
        // Valide la présence des champs obligatoires HR-Skills Pay
        request.validateContract();

        String transactionToken = tokenManager.getTransactionToken();

        String base = properties.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        // Attention : URL avec /api/v1 pour les endpoints métier (distincte de /v1/auth/transaction-token)
        String url = base + "/api/v1/payin/mobile-money";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("operator", request.operator());
        body.put("country", request.country());
        body.put("phone_number", request.phoneNumber());
        // Montant entier sans décimales selon la spécification HR-Skills Pay
        body.put("amount", request.amount().setScale(0, RoundingMode.HALF_UP).longValue());
        body.put("currency", request.currency());

        log.info("Initiating Cash-In on {} for country={}, operator={}, amount={} {}, idempotencyKey={}",
                url, request.country(), request.operator(), request.amount(), request.currency(), request.idempotencyKey());

        try {
            String responseBody = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getKeyA())
                    .header("X-Transaction-Token", transactionToken)
                    .header("Idempotency-Key", request.idempotencyKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();

            if (responseBody == null || responseBody.isBlank()) {
                throw new PaymentException("AGGREGATOR_EMPTY_RESPONSE", "Empty response from HR-Skills Pay cash-in endpoint");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String reference = text(root, "reference");
            String transactionId = text(root, "transaction_id");
            if (transactionId == null) {
                transactionId = text(root, "transactionId");
            }
            String status = text(root, "status");
            if (status == null) {
                status = "PENDING";
            }

            log.info("Cash-In initiated successfully: reference={}, transactionId={}, status={}",
                    reference, transactionId, status);

            return new CashInResponse(reference, transactionId, status);
        } catch (WebClientResponseException ex) {
            String errorBody = ex.getResponseBodyAsString();
            log.warn("HR-Skills Pay cash-in HTTP error status={}", ex.getStatusCode());
            throw mapAggregatorError(ex.getStatusCode().value(), errorBody);
        } catch (PaymentException pe) {
            throw pe;
        } catch (Exception ex) {
            log.error("HR-Skills Pay cash-in execution error: {}", ex.getMessage());
            throw new PaymentException("PAYMENT_GATEWAY_ERROR", "Cash-In initiation failed: " + ex.getMessage());
        }
    }

    private PaymentException mapAggregatorError(int httpStatus, String errorBody) {
        if (errorBody != null && !errorBody.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(errorBody);
                if (root.hasNonNull("error")) {
                    String code = root.get("error").asText();
                    String message = root.hasNonNull("message") ? root.get("message").asText() : "Aggregator rejected request";
                    return new PaymentException("AGGREGATOR_" + code, message);
                }
            } catch (Exception ignored) {
                // Pas du JSON valide, on retombe sur le statut HTTP
            }
        }
        return new PaymentException("PAYMENT_GATEWAY_ERROR", "Payment aggregator returned HTTP " + httpStatus);
    }

    private String text(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return child == null || child.isNull() ? null : child.asText();
    }
}
