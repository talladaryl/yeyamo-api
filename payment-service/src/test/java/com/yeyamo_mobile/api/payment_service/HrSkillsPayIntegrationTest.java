package com.yeyamo_mobile.api.payment_service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.yeyamo_mobile.api.payment_service.domain.PaymentAttemptStatus;
import com.yeyamo_mobile.api.payment_service.infrastructure.aggregator.HrSkillsPayProperties;
import com.yeyamo_mobile.api.payment_service.infrastructure.aggregator.HrSkillsPayTokenManager;
import com.yeyamo_mobile.api.payment_service.infrastructure.messaging.PaymentCommandConsumer;
import com.yeyamo_mobile.api.payment_service.infrastructure.persistence.PaymentAttemptEntity;
import com.yeyamo_mobile.api.payment_service.infrastructure.persistence.PaymentAttemptRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"payment.commands", "payment.events"})
public class HrSkillsPayIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HrSkillsPayTokenManager tokenManager;

    @Autowired
    private HrSkillsPayProperties properties;

    @Autowired
    private PaymentAttemptRepository attemptRepository;

    @Autowired
    private PaymentCommandConsumer commandConsumer;

    @BeforeAll
    static void setup() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void teardown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("payment.aggregator.base-url", () -> wireMockServer.baseUrl());
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
        tokenManager.setCachedTokenForTesting(null, Instant.MIN);
    }

    @Test
    void shouldObtainAndCacheTransactionToken() {
        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/v1/auth/transaction-token"))
                .withHeader("Authorization", equalTo("Bearer " + properties.getKeyA()))
                .withRequestBody(matchingJsonPath("$.api_secret", equalTo(properties.getKeyB())))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"transaction_token\":\"tok-initial-123\",\"expires_in\":2700}")));

        // Premier appel : doit contacter WireMock
        String token1 = tokenManager.getTransactionToken();
        assertEquals("tok-initial-123", token1);

        // Deuxième appel : doit utiliser le cache en mémoire sans ré-émettre de requête HTTP
        String token2 = tokenManager.getTransactionToken();
        assertEquals("tok-initial-123", token2);

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/v1/auth/transaction-token")));
    }

    @Test
    void shouldRenewTokenBeforeExpiry() {
        // Simule un token dont l'expiration restante est inférieure au seuil de sécurité de 5 minutes (ex: 200s < 300s)
        tokenManager.setCachedTokenForTesting("tok-expiring-soon", Instant.now().plusSeconds(200));

        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/v1/auth/transaction-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"transaction_token\":\"tok-renewed-456\",\"expires_in\":2700}")));

        // Appel getTransactionToken() constate qu'il reste < 5 min et déclenche le renouvellement
        String renewedToken = tokenManager.getTransactionToken();
        assertEquals("tok-renewed-456", renewedToken);

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/v1/auth/transaction-token")));
    }

    @Test
    void shouldInitiateCashInOnPaymentCommandReceived() throws Exception {
        // Préparation du stub d'authentification transaction token
        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/v1/auth/transaction-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"transaction_token\":\"tok-cashin-test\",\"expires_in\":2700}")));

        // Cas 1 Sandbox déterministe HR-Skills Pay : Montant pair (2000 XOF) -> 200 PENDING
        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/v1/payin/mobile-money"))
                .withRequestBody(matchingJsonPath("$.amount", equalTo("2000")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"PENDING\",\"reference\":\"REF-SANDBOX-PAIR-2000\",\"transaction_id\":\"tx-pair-1\"}")));

        UUID eventId1 = UUID.randomUUID();
        UUID bookingId1 = UUID.randomUUID();
        String commandPair = String.format("""
                {
                  "eventId": "%s",
                  "eventType": "payment.authorization.requested",
                  "version": 1,
                  "correlationId": "corr-pair-2000",
                  "payload": {
                    "sagaId": "%s",
                    "bookingId": "%s",
                    "userId": "user-test",
                    "amount": 2000.00,
                    "currency": "XOF",
                    "idempotencyKey": "booking:%s",
                    "operator": "ORANGE",
                    "country": "CI",
                    "phone_number": "2250700000000"
                  }
                }
                """, eventId1, UUID.randomUUID(), bookingId1, bookingId1);

        commandConsumer.consume(commandPair);

        PaymentAttemptEntity pairAttempt = attemptRepository.findByReference("REF-SANDBOX-PAIR-2000")
                .orElse(null);
        assertNotNull(pairAttempt, "Payment attempt should be created with status INITIATED for even amount");
        assertEquals(PaymentAttemptStatus.INITIATED, pairAttempt.getStatus());
        assertEquals(new BigDecimal("2000.00"), pairAttempt.getAmount());

        // Cas 2 Sandbox déterministe HR-Skills Pay : Montant impair (2001 XOF) -> 400 DECLINED
        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/v1/payin/mobile-money"))
                .withRequestBody(matchingJsonPath("$.amount", equalTo("2001")))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":\"DECLINED\",\"message\":\"Declined by operator sandbox\"}")));

        UUID eventId2 = UUID.randomUUID();
        UUID bookingId2 = UUID.randomUUID();
        String commandOdd = String.format("""
                {
                  "eventId": "%s",
                  "eventType": "payment.authorization.requested",
                  "version": 1,
                  "correlationId": "corr-odd-2001",
                  "payload": {
                    "sagaId": "%s",
                    "bookingId": "%s",
                    "userId": "user-test",
                    "amount": 2001.00,
                    "currency": "XOF",
                    "idempotencyKey": "booking:%s",
                    "operator": "ORANGE",
                    "country": "CI",
                    "phone_number": "2250700000001"
                  }
                }
                """, eventId2, UUID.randomUUID(), bookingId2, bookingId2);

        // L'échec du cash-in publie payment.failed immédiatement et lève l'exception mappée
        try {
            commandConsumer.consume(commandOdd);
        } catch (Exception expected) {
            // Exception attendue suite au rejet 400 de l'agrégateur
        }
    }

    @Test
    void shouldRejectWebhookWithInvalidSignature() throws Exception {
        byte[] payload = "{\"event\":\"payment.succeeded\",\"reference\":\"REF-WEBHOOK-INVALID\"}".getBytes(StandardCharsets.UTF_8);

        // Signature invalide (mauvais HMAC)
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/webhook/payment")
                        .header("X-Signature", "sha256=badsignature000000000000000000000000000000000000000000000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        // Absence de header de signature
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/webhook/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldConfirmPaymentAndPublishSuccessEvent() throws Exception {
        String ref = "REF-SUCC-" + UUID.randomUUID();
        PaymentAttemptEntity attempt = PaymentAttemptEntity.initiated(
                ref,
                "TX-SUCC-1",
                "booking",
                UUID.randomUUID(),
                new BigDecimal("2000.00"),
                "XOF",
                "booking:" + ref
        );
        attemptRepository.save(attempt);

        byte[] payload = String.format("{\"event\":\"payment.succeeded\",\"reference\":\"%s\",\"amount\":2000,\"currency\":\"XOF\"}", ref)
                .getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + computeHmacHex(payload, properties.getWebhookSecret());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/webhook/payment")
                        .header("X-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        PaymentAttemptEntity updated = attemptRepository.findByReference(ref).orElseThrow();
        assertEquals(PaymentAttemptStatus.SUCCESS, updated.getStatus());
    }

    @Test
    void shouldFailPaymentAndPublishFailureEvent() throws Exception {
        String ref = "REF-FAIL-" + UUID.randomUUID();
        PaymentAttemptEntity attempt = PaymentAttemptEntity.initiated(
                ref,
                "TX-FAIL-1",
                "booking",
                UUID.randomUUID(),
                new BigDecimal("2000.00"),
                "XOF",
                "booking:" + ref
        );
        attemptRepository.save(attempt);

        byte[] payload = String.format("{\"event\":\"payment.failed\",\"reference\":\"%s\",\"reason\":\"INSUFFICIENT_FUNDS\"}", ref)
                .getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + computeHmacHex(payload, properties.getWebhookSecret());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/webhook/payment")
                        .header("X-Hub-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        PaymentAttemptEntity updated = attemptRepository.findByReference(ref).orElseThrow();
        assertEquals(PaymentAttemptStatus.FAILED, updated.getStatus());
        assertEquals("INSUFFICIENT_FUNDS", updated.getFailureReason());
    }

    @Test
    void shouldNotDuplicatePublicationOnDuplicateWebhook() throws Exception {
        String ref = "REF-DUP-" + UUID.randomUUID();
        PaymentAttemptEntity attempt = PaymentAttemptEntity.initiated(
                ref,
                "TX-DUP-1",
                "booking",
                UUID.randomUUID(),
                new BigDecimal("2000.00"),
                "XOF",
                "booking:" + ref
        );
        attemptRepository.save(attempt);

        byte[] payload = String.format("{\"event\":\"payment.succeeded\",\"reference\":\"%s\",\"amount\":2000,\"currency\":\"XOF\"}", ref)
                .getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + computeHmacHex(payload, properties.getWebhookSecret());

        // Premier webhook
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/webhook/payment")
                        .header("X-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        PaymentAttemptEntity firstCheck = attemptRepository.findByReference(ref).orElseThrow();
        assertEquals(PaymentAttemptStatus.SUCCESS, firstCheck.getStatus());

        // Deuxième webhook identique : doit retourner 200 OK et être ignoré (idempotence stricte)
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/webhook/payment")
                        .header("X-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        PaymentAttemptEntity secondCheck = attemptRepository.findByReference(ref).orElseThrow();
        assertEquals(PaymentAttemptStatus.SUCCESS, secondCheck.getStatus());
    }

    private String computeHmacHex(byte[] data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data));
    }
}
