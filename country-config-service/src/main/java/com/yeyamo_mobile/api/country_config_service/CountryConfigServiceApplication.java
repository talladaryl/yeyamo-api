package com.yeyamo_mobile.api.country_config_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Country Configuration Service
 * 
 * Centralizes multi-country, multi-language, multi-currency configuration.
 * Provides country-aware features, launch status, and business rules.
 * 
 * Cameroon-first strategy with LIVE status at launch.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
@EnableCaching
public class CountryConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CountryConfigServiceApplication.class, args);
    }
}
