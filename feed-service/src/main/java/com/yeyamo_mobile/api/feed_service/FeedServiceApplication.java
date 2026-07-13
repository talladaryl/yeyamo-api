package com.yeyamo_mobile.api.feed_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class FeedServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeedServiceApplication.class, args);
    }
}
