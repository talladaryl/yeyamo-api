package com.yeyamo_mobile.api.campaign_service.infrastructure.adapter;

import com.yeyamo_mobile.api.campaign_service.domain.port.PartnerValidationPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PartnerValidationAdapter implements PartnerValidationPort {
    
    private static final Logger log = LoggerFactory.getLogger(PartnerValidationAdapter.class);
    
    private final RestTemplate restTemplate;
    private final String partnerServiceUrl;

    public PartnerValidationAdapter(
            RestTemplate restTemplate,
            @Value("${yeyamo.services.partner-service.url}") String partnerServiceUrl) {
        this.restTemplate = restTemplate;
        this.partnerServiceUrl = partnerServiceUrl;
    }

    @Override
    @CircuitBreaker(name = "partnerValidation", fallbackMethod = "fallbackIsValidPartner")
    public boolean isValidPartner(String partnerId) {
        try {
            String url = partnerServiceUrl + "/api/v1/partners/" + partnerId + "/validation-status";
            ResponseEntity<PartnerValidationResponse> response = restTemplate.getForEntity(
                    url,
                    PartnerValidationResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().isValidated();
            }
            
            log.warn("Partner validation returned unexpected response for partnerId={}", partnerId);
            return false;
        } catch (Exception e) {
            log.error("Error validating partner partnerId={}", partnerId, e);
            return false;
        }
    }

    private boolean fallbackIsValidPartner(String partnerId, Throwable throwable) {
        log.error("Circuit breaker fallback for partner validation partnerId={}", partnerId, throwable);
        return false;
    }

    private static class PartnerValidationResponse {
        private boolean validated;

        public boolean isValidated() { return validated; }
        public void setValidated(boolean validated) { this.validated = validated; }
    }
}
