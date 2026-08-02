package com.yeyamo_mobile.api.campaign_service.infrastructure.adapter;

import com.yeyamo_mobile.api.campaign_service.domain.model.PromotedEntityType;
import com.yeyamo_mobile.api.campaign_service.domain.port.PromotedEntityValidationPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PromotedEntityValidationAdapter implements PromotedEntityValidationPort {
    
    private static final Logger log = LoggerFactory.getLogger(PromotedEntityValidationAdapter.class);
    
    private final RestTemplate restTemplate;
    private final String placeServiceUrl;
    private final String eventServiceUrl;
    private final String contentServiceUrl;
    private final String partnerServiceUrl;

    public PromotedEntityValidationAdapter(
            RestTemplate restTemplate,
            @Value("${yeyamo.services.place-service.url}") String placeServiceUrl,
            @Value("${yeyamo.services.event-service.url}") String eventServiceUrl,
            @Value("${yeyamo.services.content-service.url}") String contentServiceUrl,
            @Value("${yeyamo.services.partner-service.url}") String partnerServiceUrl) {
        this.restTemplate = restTemplate;
        this.placeServiceUrl = placeServiceUrl;
        this.eventServiceUrl = eventServiceUrl;
        this.contentServiceUrl = contentServiceUrl;
        this.partnerServiceUrl = partnerServiceUrl;
    }

    @Override
    @CircuitBreaker(name = "entityValidation", fallbackMethod = "fallbackExists")
    public boolean exists(PromotedEntityType type, String entityId) {
        try {
            String url = buildUrl(type, entityId);
            ResponseEntity<Void> response = restTemplate.getForEntity(url, Void.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("Error validating entity type={}, id={}", type, entityId, e);
            return false;
        }
    }

    private boolean fallbackExists(PromotedEntityType type, String entityId, Throwable throwable) {
        log.error("Circuit breaker fallback for entity validation type={}, id={}", type, entityId, throwable);
        return false;
    }

    private String buildUrl(PromotedEntityType type, String entityId) {
        return switch (type) {
            case PLACE -> placeServiceUrl + "/api/v1/places/" + entityId;
            case EVENT -> eventServiceUrl + "/api/v1/events/" + entityId;
            case POST -> contentServiceUrl + "/api/v1/posts/" + entityId;
            case PARTNER_PROFILE -> partnerServiceUrl + "/api/v1/partners/" + entityId;
            case EXPERIENCE -> placeServiceUrl + "/api/v1/experiences/" + entityId;
        };
    }
}
