package com.yeyamo_mobile.api.discovery_service.infrastructure.maps;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="google.maps")public record GoogleMapsProperties(String apiKey,String baseUrl,String routesBaseUrl){public GoogleMapsProperties{baseUrl=baseUrl==null||baseUrl.isBlank()?"https://maps.googleapis.com":baseUrl;routesBaseUrl=routesBaseUrl==null||routesBaseUrl.isBlank()?"https://routes.googleapis.com":routesBaseUrl;if(apiKey==null||apiKey.isBlank())throw new IllegalStateException("GOOGLE_MAPS_SERVER_API_KEY is required");}}
