package com.yeyamo_mobile.api.discovery_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

import com.yeyamo_mobile.api.discovery_service.infrastructure.maps.GoogleMapsProperties;
import com.yeyamo_mobile.shared.country.CountryConfigClient;

@EnableDiscoveryClient
@SpringBootApplication
@EnableConfigurationProperties(GoogleMapsProperties.class)
@Import(CountryConfigClient.class)
public class DiscoveryServiceApplication {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}
