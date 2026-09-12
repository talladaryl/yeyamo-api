package com.yeyamo_mobile.api.culture_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.web.client.RestClient;
import com.yeyamo_mobile.shared.country.CountryConfigClient;

@SpringBootApplication
@EnableScheduling
@Import(CountryConfigClient.class)
public class CultureServiceApplication {
    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    public static void main(String[] args) {
        SpringApplication.run(CultureServiceApplication.class, args);
    }
}
