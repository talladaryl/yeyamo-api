package com.yeyamo_mobile.api.mission_reward_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class MissionRewardServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MissionRewardServiceApplication.class, args);
    }
}
