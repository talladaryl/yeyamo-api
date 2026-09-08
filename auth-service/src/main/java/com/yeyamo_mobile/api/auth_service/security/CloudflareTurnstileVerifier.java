package com.yeyamo_mobile.api.auth_service.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CloudflareTurnstileVerifier implements AntiBotVerifier {
    private static final Logger log = LoggerFactory.getLogger(CloudflareTurnstileVerifier.class);
    private final RestClient restClient;
    private final TurnstileProperties properties;

    public CloudflareTurnstileVerifier(RestClient restClient, TurnstileProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public void verify(String token, AntiBotAction action, String clientIp) {
        if (!properties.enabled()) return;
        if (token == null || token.isBlank()) {
            throw new ApiException("TURNSTILE_REQUIRED", "Vérification de sécurité requise", HttpStatus.FORBIDDEN);
        }
        if (properties.secretKey() == null || properties.secretKey().isBlank()) {
            throw unavailable();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", properties.secretKey());
        form.add("response", token);
        if (clientIp != null && !clientIp.isBlank()) form.add("remoteip", clientIp);

        TurnstileSiteVerifyResponse response;
        try {
            response = restClient.post()
                    .uri(properties.verifyUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TurnstileSiteVerifyResponse.class);
        } catch (RestClientException exception) {
            log.warn("Turnstile verification provider is unavailable; allowing request for availability", exception);
            return;
        }

        if (response == null) {
            log.warn("Turnstile verification provider returned an empty response; allowing request for availability");
            return;
        }
        if (!response.success()) {
            throw new ApiException("TURNSTILE_VERIFICATION_FAILED", "Vérification de sécurité invalide", HttpStatus.BAD_REQUEST);
        }
        if (!properties.expectedHostname().equalsIgnoreCase(response.hostname())) {
            throw new ApiException("TURNSTILE_HOSTNAME_MISMATCH", "Origine du challenge invalide", HttpStatus.FORBIDDEN);
        }
        if (!action.value().equals(response.action())) {
            throw new ApiException("TURNSTILE_ACTION_MISMATCH", "Action du challenge invalide", HttpStatus.FORBIDDEN);
        }
    }

    private ApiException unavailable() {
        return new ApiException("TURNSTILE_PROVIDER_UNAVAILABLE",
                "Le service de vérification est temporairement indisponible", HttpStatus.SERVICE_UNAVAILABLE);
    }

}
