package com.yeyamo_mobile.api.place_service.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.yeyamo_mobile.api.place_service.config.OpenRouteServiceProperties;
import com.yeyamo_mobile.api.place_service.dto.DirectionsResponse;
import com.yeyamo_mobile.api.place_service.exception.ApiException;

@Service
public class DirectionsService {
    private final RestClient client;
    private final OpenRouteServiceProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    public DirectionsService(RestClient openRouteServiceRestClient, OpenRouteServiceProperties properties, ObjectMapper objectMapper) {
        this.client = openRouteServiceRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public DirectionsResponse directions(double originLat, double originLng, double destLat, double destLng, String mode) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw unavailable();
        }
        CacheKey key = CacheKey.of(originLat, originLng, destLat, destLng, mode);
        Instant now = Instant.now();
        CacheEntry result = cache.compute(key, (ignored, current) -> current != null && current.expiresAt().isAfter(now)
                ? current : new CacheEntry(request(originLat, originLng, destLat, destLng, mode), now.plus(properties.cacheTtl())));
        return result.value();
    }

    private DirectionsResponse request(double originLat, double originLng, double destLat, double destLng, String mode) {
        try {
            String payload = client.get()
                    .uri(uri -> uri.path("/v2/directions/{mode}")
                            .queryParam("start", originLng + "," + originLat)
                            .queryParam("end", destLng + "," + destLat)
                            .build(mode))
                    .header("Authorization", properties.apiKey())
                    .retrieve()
                    .body(String.class);
            JsonNode root = payload == null ? null : objectMapper.readTree(payload);
            JsonNode feature = root == null ? null : root.path("features").path(0);
            JsonNode summary = feature == null ? null : feature.path("properties").path("summary");
            JsonNode coordinates = feature == null ? null : feature.path("geometry").path("coordinates");
            if (summary == null || summary.isMissingNode() || !coordinates.isArray()) throw unavailable();
            List<DirectionsResponse.Coordinate> geometry = coordinates.valueStream()
                    .filter(JsonNode::isArray)
                    .filter(node -> node.size() >= 2)
                    .map(node -> new DirectionsResponse.Coordinate(node.get(0).asDouble(), node.get(1).asDouble()))
                    .toList();
            if (geometry.isEmpty()) throw unavailable();
            return new DirectionsResponse(summary.path("distance").asDouble(), summary.path("duration").asDouble(), geometry);
        } catch (RestClientResponseException exception) {
            HttpStatusCode status = exception.getStatusCode();
            if (status.value() == 429) throw unavailable();
            throw unavailable();
        } catch (RestClientException | JsonProcessingException exception) {
            throw unavailable();
        }
    }

    private ApiException unavailable() {
        return new ApiException("ROUTING_UNAVAILABLE", "Calcul d’itinéraire temporairement indisponible", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private record CacheKey(double originLat, double originLng, double destLat, double destLng, String mode) {
        static CacheKey of(double originLat, double originLng, double destLat, double destLng, String mode) {
            return new CacheKey(round(originLat), round(originLng), round(destLat), round(destLng), mode.toLowerCase(Locale.ROOT));
        }
        private static double round(double value) { return Math.round(value * 100_000d) / 100_000d; }
    }
    private record CacheEntry(DirectionsResponse value, Instant expiresAt) { }
}
