package com.yeyamo_mobile.api.messaging_service.infrastructure.client;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.yeyamo_mobile.api.messaging_service.domain.MessagingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartnerIdentityClientTest {

    private static final String TEST_TOKEN = "secret-internal-key-123";

    @Test
    @DisplayName("Devrait résoudre l'identifiant utilisateur avec le header X-Internal-Token")
    void shouldResolveUserIdSuccessfully() {
        UUID partnerId = UUID.randomUUID();
        AtomicReference<String> tokenHeader = new AtomicReference<>();
        AtomicReference<URI> requestUri = new AtomicReference<>();

        ExchangeFunction exchangeFunction = request -> {
            tokenHeader.set(request.headers().getFirst("X-Internal-Token"));
            requestUri.set(request.url());
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"userId\":\"artisan-user-789\"}")
                    .build());
        };

        WebClient webClient = WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();

        PartnerIdentityClient client = new PartnerIdentityClient(webClient, TEST_TOKEN);

        String userId = client.resolveUserId(partnerId);

        assertThat(userId).isEqualTo("artisan-user-789");
        assertThat(tokenHeader.get()).isEqualTo(TEST_TOKEN);
        assertThat(requestUri.get().toString()).contains("/internal/partners/" + partnerId + "/user-id");
    }

    @Test
    @DisplayName("Devrait lever PARTNER_NOT_FOUND quand le partner-service renvoie 404")
    void shouldThrowPartnerNotFoundOn404() {
        UUID partnerId = UUID.randomUUID();

        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"code\":\"PARTNER_NOT_FOUND\",\"message\":\"Partenaire introuvable\"}")
                .build());

        WebClient webClient = WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();

        PartnerIdentityClient client = new PartnerIdentityClient(webClient, TEST_TOKEN);

        assertThatThrownBy(() -> client.resolveUserId(partnerId))
                .isInstanceOf(MessagingException.class)
                .satisfies(ex -> {
                    MessagingException me = (MessagingException) ex;
                    assertThat(me.getCode()).isEqualTo("PARTNER_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("Devrait lever PARTNER_RESOLUTION_FAILED quand partner-service renvoie 500")
    void shouldThrowPartnerResolutionFailedOn500() {
        UUID partnerId = UUID.randomUUID();

        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"code\":\"INTERNAL_ERROR\",\"message\":\"Crash interne\"}")
                .build());

        WebClient webClient = WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();

        PartnerIdentityClient client = new PartnerIdentityClient(webClient, TEST_TOKEN);

        assertThatThrownBy(() -> client.resolveUserId(partnerId))
                .isInstanceOf(MessagingException.class)
                .satisfies(ex -> {
                    MessagingException me = (MessagingException) ex;
                    assertThat(me.getCode()).isEqualTo("PARTNER_RESOLUTION_FAILED");
                    // Do not leak internal error
                    assertThat(me.getMessage()).doesNotContain("Crash interne");
                });
    }

    @Test
    @DisplayName("Devrait lever INVALID_PARTNER_ID si partnerId est null")
    void shouldThrowInvalidPartnerIdWhenNull() {
        PartnerIdentityClient client = new PartnerIdentityClient(WebClient.builder().build(), TEST_TOKEN);

        assertThatThrownBy(() -> client.resolveUserId(null))
                .isInstanceOf(MessagingException.class)
                .satisfies(ex -> assertThat(((MessagingException) ex).getCode()).isEqualTo("INVALID_PARTNER_ID"));
    }
}
