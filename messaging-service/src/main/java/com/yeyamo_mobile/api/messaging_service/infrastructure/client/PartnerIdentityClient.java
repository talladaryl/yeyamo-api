package com.yeyamo_mobile.api.messaging_service.infrastructure.client;

import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.yeyamo_mobile.api.messaging_service.domain.MessagingException;

@Component
public class PartnerIdentityClient {

    private static final Logger log = LoggerFactory.getLogger(PartnerIdentityClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;
    private final String internalToken;

    @Autowired
    public PartnerIdentityClient(
            @Autowired(required = false) WebClient.Builder webClientBuilder,
            @Value("${yeyamo.services.partner.url:http://partner-service:8101}") String partnerServiceUrl,
            @Value("${yeyamo.security.internal-token:${INTERNAL_SERVICE_TOKEN:}}") String internalToken
    ) {
        WebClient.Builder builder = webClientBuilder != null ? webClientBuilder : WebClient.builder();
        this.webClient = builder.baseUrl(partnerServiceUrl).build();
        this.internalToken = internalToken != null ? internalToken.trim() : "";
    }

    public PartnerIdentityClient(WebClient webClient, String internalToken) {
        this.webClient = webClient;
        this.internalToken = internalToken != null ? internalToken.trim() : "";
    }

    public String resolveUserId(UUID partnerId) {
        if (partnerId == null) {
            throw new MessagingException("INVALID_PARTNER_ID", "L'identifiant du partenaire est requis");
        }

        try {
            PartnerUserIdResponse response = webClient.get()
                    .uri("/internal/partners/{partnerId}/user-id", partnerId)
                    .header("X-Internal-Token", internalToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        HttpStatusCode status = clientResponse.statusCode();
                        if (status.equals(HttpStatus.NOT_FOUND)) {
                            return clientResponse.createException().map(ex -> new MessagingException("PARTNER_NOT_FOUND", "Partenaire introuvable ou indisponible"));
                        }
                        if (status.equals(HttpStatus.UNAUTHORIZED) || status.equals(HttpStatus.FORBIDDEN)) {
                            log.error("Internal service authentication failed when calling partner identity resolution");
                            return clientResponse.createException().map(ex -> new MessagingException("PARTNER_RESOLUTION_FAILED", "Impossible de contacter ce partenaire pour le moment"));
                        }
                        return clientResponse.createException().map(ex -> new MessagingException("PARTNER_RESOLUTION_FAILED", "Impossible de contacter ce partenaire pour le moment"));
                    })
                    .bodyToMono(PartnerUserIdResponse.class)
                    .timeout(TIMEOUT)
                    .block();

            if (response == null || response.userId() == null || response.userId().isBlank()) {
                throw new MessagingException("PARTNER_NOT_FOUND", "Partenaire introuvable ou indisponible");
            }

            return response.userId();
        } catch (MessagingException me) {
            throw me;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw new MessagingException("PARTNER_NOT_FOUND", "Partenaire introuvable ou indisponible");
            }
            log.warn("Partner service returned error: status={}", e.getStatusCode());
            throw new MessagingException("PARTNER_RESOLUTION_FAILED", "Impossible de contacter ce partenaire pour le moment");
        } catch (Exception e) {
            log.warn("Partner service call failed or timed out: {}", e.getMessage());
            throw new MessagingException("PARTNER_RESOLUTION_FAILED", "Impossible de contacter ce partenaire pour le moment");
        }
    }

    public record PartnerUserIdResponse(String userId) {}
}
