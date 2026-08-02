package com.yeyamo_mobile.api.feed_service.infrastructure.ads;

import com.yeyamo_mobile.api.feed_service.application.port.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Resilient client for ads-delivery-service with circuit breaker, timeout, and fallback
 */
@Component
@ConditionalOnProperty(name = "yeyamo.ads.enabled", havingValue = "true", matchIfMissing = false)
public class ResilientAdsDeliveryClient implements AdsDeliveryPort {

    private static final Logger logger = LoggerFactory.getLogger(ResilientAdsDeliveryClient.class);
    
    private final RestClient restClient;
    private final long timeoutMs;
    private final CircuitBreaker circuitBreaker;
    
    public ResilientAdsDeliveryClient(
            @Value("${yeyamo.ads.delivery-service.url:http://ads-delivery-service:8080}") String baseUrl,
            @Value("${yeyamo.ads.timeout-ms:500}") long timeoutMs,
            @Value("${yeyamo.ads.circuit-breaker.failure-threshold:5}") int failureThreshold,
            @Value("${yeyamo.ads.circuit-breaker.timeout-duration-ms:30000}") long circuitBreakerTimeout) {
        
        this.timeoutMs = timeoutMs;
        this.circuitBreaker = new CircuitBreaker(failureThreshold, circuitBreakerTimeout);
        
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        
        logger.info("Initialized ResilientAdsDeliveryClient: baseUrl={}, timeout={}ms", baseUrl, timeoutMs);
    }

    @Override
    public Optional<List<SponsoredPlacement>> requestPlacements(AdSelectionRequest request) {
        if (!circuitBreaker.allowRequest()) {
            logger.warn("Circuit breaker OPEN - skipping ads request for user: {}", request.userId());
            return Optional.empty();
        }
        
        long start = System.currentTimeMillis();
        try {
            logger.debug("Requesting ads: userId={}, placement={}, maxAds={}", 
                request.userId(), request.placementType(), request.maxAds());
            
            var response = restClient.post()
                    .uri("/api/v1/ads/select")
                    .header("X-Correlation-ID", request.correlationId())
                    .body(buildRequestBody(request))
                    .retrieve()
                    .toEntity(AdSelectionResponse.class);
            
            long duration = System.currentTimeMillis() - start;
            
            if (duration > timeoutMs) {
                logger.warn("Ads request exceeded timeout: {}ms > {}ms", duration, timeoutMs);
                circuitBreaker.recordFailure();
                return Optional.empty();
            }
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                circuitBreaker.recordSuccess();
                List<SponsoredPlacement> placements = mapToPlacements(response.getBody());
                logger.info("Received {} sponsored placements in {}ms", placements.size(), duration);
                return Optional.of(placements);
            }
            
            circuitBreaker.recordFailure();
            return Optional.empty();
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            logger.error("Ads request failed after {}ms: {}", duration, e.getMessage());
            circuitBreaker.recordFailure();
            return Optional.empty();
        }
    }

    @Override
    public boolean isHealthy() {
        return circuitBreaker.allowRequest();
    }
    
    private Map<String, Object> buildRequestBody(AdSelectionRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", request.userId());
        body.put("placementType", request.placementType());
        body.put("maxAds", request.maxAds());
        body.put("contextType", request.contextType());
        if (request.excludedCampaigns() != null && !request.excludedCampaigns().isEmpty()) {
            body.put("excludedCampaigns", request.excludedCampaigns());
        }
        return body;
    }
    
    private List<SponsoredPlacement> mapToPlacements(AdSelectionResponse response) {
        if (response.placements() == null) {
            return List.of();
        }
        
        return response.placements().stream()
                .map(p -> new SponsoredPlacement(
                    p.deliveryId(),
                    p.campaignId(),
                    p.promotedEntityType(),
                    p.promotedEntityId(),
                    p.creative(),
                    p.bidAmount(),
                    p.trackingToken()
                ))
                .toList();
    }
    
    // Internal response mapping
    private record AdSelectionResponse(List<PlacementDto> placements) {}
    
    private record PlacementDto(
        String deliveryId,
        String campaignId,
        String promotedEntityType,
        String promotedEntityId,
        Map<String, Object> creative,
        java.math.BigDecimal bidAmount,
        String trackingToken
    ) {}
    
    /**
     * Simple circuit breaker implementation
     */
    private static class CircuitBreaker {
        private enum State { CLOSED, OPEN, HALF_OPEN }
        
        private final int failureThreshold;
        private final long timeoutDurationMs;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private volatile State state = State.CLOSED;
        
        CircuitBreaker(int failureThreshold, long timeoutDurationMs) {
            this.failureThreshold = failureThreshold;
            this.timeoutDurationMs = timeoutDurationMs;
        }
        
        boolean allowRequest() {
            if (state == State.CLOSED) {
                return true;
            }
            
            if (state == State.OPEN) {
                long now = System.currentTimeMillis();
                if (now - lastFailureTime.get() > timeoutDurationMs) {
                    logger.info("Circuit breaker: OPEN -> HALF_OPEN");
                    state = State.HALF_OPEN;
                    return true;
                }
                return false;
            }
            
            // HALF_OPEN
            return true;
        }
        
        void recordSuccess() {
            failureCount.set(0);
            successCount.incrementAndGet();
            
            if (state == State.HALF_OPEN) {
                logger.info("Circuit breaker: HALF_OPEN -> CLOSED");
                state = State.CLOSED;
            }
        }
        
        void recordFailure() {
            int failures = failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
            
            if (failures >= failureThreshold && state != State.OPEN) {
                logger.warn("Circuit breaker: {} -> OPEN (failures={})", state, failures);
                state = State.OPEN;
            }
        }
    }
}
