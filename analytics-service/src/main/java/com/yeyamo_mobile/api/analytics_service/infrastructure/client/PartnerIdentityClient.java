package com.yeyamo_mobile.api.analytics_service.infrastructure.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Resolves the partner from the caller's own bearer token, never from a client supplied ID. */
@Component
public class PartnerIdentityClient {
    private final RestClient partners;
    public PartnerIdentityClient(RestClient.Builder builder,
            @Value("${yeyamo.services.partner.url:http://partner-service:8101}") String partnerUrl) {
        partners = builder.baseUrl(partnerUrl).build();
    }

    public String currentPartnerId(String bearerToken) {
        try {
            PartnerMe partner = partners.get().uri("/api/v1/partners/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken).retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new IllegalStateException("PARTNER_NOT_FOUND");
                    }).body(PartnerMe.class);
            if (partner == null || partner.id() == null) throw new IllegalStateException("PARTNER_NOT_FOUND");
            return partner.id().toString();
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("PARTNER_IDENTITY_UNAVAILABLE");
        }
    }
    private record PartnerMe(UUID id) { }
}
