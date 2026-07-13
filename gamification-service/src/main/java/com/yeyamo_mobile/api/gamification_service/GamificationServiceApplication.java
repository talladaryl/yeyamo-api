package com.yeyamo_mobile.api.gamification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class GamificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GamificationServiceApplication.class, args);
    }
}
