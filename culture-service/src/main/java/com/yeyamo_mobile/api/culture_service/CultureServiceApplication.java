package com.yeyamo_mobile.api.culture_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CultureServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CultureServiceApplication.class, args);
    }
}
