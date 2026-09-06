package com.yeyamo_mobile.api.payment_service.infrastructure.aggregator;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.payment_service.domain.PaymentException;

@Component
public class HrSkillsPayTokenManager {

    private static final Logger log = LoggerFactory.getLogger(HrSkillsPayTokenManager.class);
    private static final long SAFETY_BUFFER_SECONDS = 300; // 5 minutes
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final HrSkillsPayProperties properties;
    private final ObjectMapper objectMapper;
    private final ReentrantLock lock = new ReentrantLock();

    private String cachedToken;
    private Instant tokenExpiresAt = Instant.MIN;

    @Autowired
    public HrSkillsPayTokenManager(
            @Autowired(required = false) WebClient.Builder webClientBuilder,
            HrSkillsPayProperties properties,
            ObjectMapper objectMapper) {
        this.webClient = webClientBuilder != null ? webClientBuilder.build() : WebClient.builder().build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public HrSkillsPayTokenManager(
            WebClient webClient,
            HrSkillsPayProperties properties,
            ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String getTransactionToken() {
        Instant now = Instant.now();
        if (cachedToken != null && tokenExpiresAt.isAfter(now.plusSeconds(SAFETY_BUFFER_SECONDS))) {
            return cachedToken;
        }

        lock.lock();
        try {
            now = Instant.now();
            if (cachedToken != null && tokenExpiresAt.isAfter(now.plusSeconds(SAFETY_BUFFER_SECONDS))) {
                return cachedToken;
            }
            renewToken();
            return cachedToken;
        } finally {
            lock.unlock();
        }
    }

    private void renewToken() {
        String base = properties.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        // Attention : URL littérale SANS /api contrairement aux autres endpoints
        String url = base + "/v1/auth/transaction-token";

        log.info("Renewing HR-Skills Pay transaction token from endpoint {}", url);

        try {
            Map<String, String> requestBody = Map.of("api_secret", properties.getKeyB());

            String responseBody = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getKeyA())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (responseBody == null || responseBody.isBlank()) {
                throw new PaymentException("TOKEN_RENEWAL_FAILED", "Empty response received from aggregator token endpoint");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String token = root.hasNonNull("transaction_token") 
                    ? root.get("transaction_token").asText() 
                    : (root.hasNonNull("token") ? root.get("token").asText() : null);

            if (token == null || token.isBlank()) {
                throw new PaymentException("TOKEN_RENEWAL_FAILED", "No transaction_token found in token response");
            }

            long expiresIn = root.hasNonNull("expires_in") ? root.get("expires_in").asLong(2700) : 2700;

            this.cachedToken = token;
            this.tokenExpiresAt = Instant.now().plusSeconds(expiresIn);

            log.info("HR-Skills Pay transaction token successfully renewed, expires in {}s (at {})",
                    expiresIn, tokenExpiresAt);
        } catch (PaymentException pe) {
            throw pe;
        } catch (Exception ex) {
            log.error("Failed to renew transaction token from {}. KeyA masked: {}, Error: {}",
                    url, maskSecret(properties.getKeyA()), ex.getMessage());
            throw new PaymentException("TOKEN_RENEWAL_FAILED", "Failed to renew HR-Skills Pay transaction token: " + ex.getMessage());
        }
    }

    public static String maskSecret(String secret) {
        if (secret == null || secret.isBlank()) return "[EMPTY]";
        if (secret.length() <= 6) return "***";
        return secret.substring(0, 3) + "***" + secret.substring(secret.length() - 3);
    }

    // Méthodes pour inspection/contrôle dans les tests unitaires et d'intégration
    public void setCachedTokenForTesting(String token, Instant expiresAt) {
        this.cachedToken = token;
        this.tokenExpiresAt = expiresAt;
    }

    public Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public String getCachedToken() {
        return cachedToken;
    }
}
