package com.yeyamo_mobile.api.culture_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Import;
import com.yeyamo_mobile.shared.country.CountryConfigClient;

@SpringBootApplication
@EnableScheduling
@Import(CountryConfigClient.class)
public class CultureServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CultureServiceApplication.class, args);
    }
}
