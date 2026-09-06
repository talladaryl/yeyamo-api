package com.yeyamo_mobile.api.user_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import com.yeyamo_mobile.shared.country.CountryConfigClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@Import(CountryConfigClient.class)
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

}
