package com.yeyamo_mobile.api.discovery_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.yeyamo_mobile.api.discovery_service.infrastructure.maps.GoogleMapsProperties;

@EnableDiscoveryClient
@SpringBootApplication
@EnableConfigurationProperties(GoogleMapsProperties.class)
public class DiscoveryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}
