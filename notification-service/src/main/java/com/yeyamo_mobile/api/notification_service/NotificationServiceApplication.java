package com.yeyamo_mobile.api.notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.yeyamo_mobile.api.notification_service.infrastructure.channel.EmailSenderProperties;

@SpringBootApplication
@EnableConfigurationProperties(EmailSenderProperties.class)
@EnableDiscoveryClient
@EnableScheduling
public class NotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}

}
