package com.yeyamo_mobile.api.booking_service.infrastructure.client;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.yeyamo_mobile.api.booking_service.domain.BookingException;

/** Reads canonical experience data; mobile input never decides price, currency or owner. */
@Component
public class ExperienceCatalogClient {
    private final RestClient catalog;
    private final RestClient partners;
    private final String internalToken;

    public ExperienceCatalogClient(RestClient.Builder builder,
            @Value("${yeyamo.services.catalog.url:http://catalog-service:8091}") String catalogUrl,
            @Value("${yeyamo.services.partner.url:http://partner-service:8101}") String partnerUrl,
            @Value("${yeyamo.security.internal-token:${INTERNAL_SERVICE_TOKEN:}}") String internalToken) {
        catalog = builder.baseUrl(catalogUrl).build();
        partners = builder.baseUrl(partnerUrl).build();
        this.internalToken = internalToken;
    }

    public CanonicalExperience requireExperience(String experienceId) {
        try {
            CanonicalExperience result = catalog.get().uri("/api/v1/catalog/assets/{id}", experienceId).retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new BookingException("EXPERIENCE_NOT_AVAILABLE", "L'experience n'est pas disponible");
                    }).body(CanonicalExperience.class);
            if (result == null || !"EXPERIENCE".equals(result.type()) || result.ownerId() == null || result.price() == null
                    || result.price().signum() < 0 || result.currency() == null || !result.currency().matches("[A-Za-z]{3}")) {
                throw new BookingException("EXPERIENCE_NOT_BOOKABLE", "L'experience ne possede pas un tarif canonique reservable");
            }
            return result;
        } catch (BookingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BookingException("EXPERIENCE_CATALOG_UNAVAILABLE", "Le catalogue des experiences est indisponible");
        }
    }

    public void requirePartnerOwner(UUID partnerId, String actor) {
        try {
            PartnerAuthorization authorization = partners.get()
                    .uri("/internal/partners/{partnerId}/users/{userId}/artwork-management", partnerId, actor)
                    .header("X-Internal-Token", internalToken).retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new BookingException("EXPERIENCE_OWNER_FORBIDDEN", "Vous ne gerez pas cette experience");
                    }).body(PartnerAuthorization.class);
            if (authorization == null || !authorization.allowed() || !partnerId.equals(authorization.partnerId())) {
                throw new BookingException("EXPERIENCE_OWNER_FORBIDDEN", "Vous ne gerez pas cette experience");
            }
        } catch (BookingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BookingException("EXPERIENCE_OWNER_VALIDATION_UNAVAILABLE", "La validation du proprietaire est indisponible");
        }
    }

    public record CanonicalExperience(UUID id, String type, UUID ownerId, String countryCode, BigDecimal price,
            String currency, Integer capacityMin, Integer capacityMax, UUID placeId) { }
    public record PartnerAuthorization(UUID partnerId, boolean allowed) { }
}
