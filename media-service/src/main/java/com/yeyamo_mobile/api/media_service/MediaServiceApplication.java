package com.yeyamo_mobile.api.media_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.yeyamo_mobile.api.media_service.infrastructure.storage.R2StorageProperties;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableConfigurationProperties(R2StorageProperties.class)
public class MediaServiceApplication {
    public static void main(String[] args) { SpringApplication.run(MediaServiceApplication.class, args); }
}
