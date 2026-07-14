package com.yeyamo_mobile.api.moderation_trust_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDiscoveryClient
@SpringBootApplication
@EnableScheduling
public class ModerationTrustServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModerationTrustServiceApplication.class, args);
    }
}
