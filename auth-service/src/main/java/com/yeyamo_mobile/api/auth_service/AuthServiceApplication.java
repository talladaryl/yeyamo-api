package com.yeyamo_mobile.api.auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.yeyamo_mobile.api.auth_service.service.EmailSenderProperties;
import com.yeyamo_mobile.api.auth_service.security.TurnstileProperties;

@SpringBootApplication
@EnableConfigurationProperties({EmailSenderProperties.class, TurnstileProperties.class})
@EnableDiscoveryClient
@EnableScheduling
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
