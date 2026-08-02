package com.yeyamo_mobile.api.ads_delivery_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableKafka
@EnableScheduling
public class AdsDeliveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdsDeliveryServiceApplication.class, args);
    }
}
