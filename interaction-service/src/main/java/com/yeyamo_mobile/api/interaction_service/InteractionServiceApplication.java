package com.yeyamo_mobile.api.interaction_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@EnableDiscoveryClient
@SpringBootApplication
@EnableScheduling
public class InteractionServiceApplication {
    @Bean
    RestClient.Builder restClientBuilder() { return RestClient.builder(); }
    public static void main(String[] args) {
        SpringApplication.run(InteractionServiceApplication.class, args);
    }
}
