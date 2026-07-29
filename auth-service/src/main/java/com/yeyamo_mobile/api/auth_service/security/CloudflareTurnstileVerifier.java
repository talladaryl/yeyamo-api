package com.yeyamo_mobile.api.auth_service.security;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;

@Component
public class CloudflareTurnstileVerifier implements AntiBotVerifier {
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
            throw unavailable();
        }

        if (response == null) throw unavailable();
        if (!response.success()) {
            List<String> errors = response.errorCodes() == null ? List.of() : response.errorCodes();
            String code = errors.contains("timeout-or-duplicate") ? "TURNSTILE_EXPIRED" : "TURNSTILE_INVALID";
            throw new ApiException(code, "Vérification de sécurité invalide", HttpStatus.FORBIDDEN);
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
