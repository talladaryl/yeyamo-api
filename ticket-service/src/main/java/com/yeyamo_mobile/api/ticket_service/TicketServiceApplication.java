package com.yeyamo_mobile.api.ticket_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.yeyamo_mobile.api.ticket_service.infrastructure.messaging.TicketProcessedEvent;
import com.yeyamo_mobile.api.ticket_service.infrastructure.messaging.TicketProcessedEventRepository;
import com.yeyamo_mobile.api.ticket_service.infrastructure.outbox.SpringOutboxRepository;
import com.yeyamo_mobile.api.ticket_service.infrastructure.outbox.TicketOutboxEntity;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.SpringTicketSaleConfigurationRepository;
import com.yeyamo_mobile.api.ticket_service.infrastructure.persistence.TicketSaleConfigurationEntity;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EntityScan(basePackageClasses = {
        TicketSaleConfigurationEntity.class,
        TicketOutboxEntity.class,
        TicketProcessedEvent.class
})
@EnableJpaRepositories(basePackageClasses = {
        SpringTicketSaleConfigurationRepository.class,
        SpringOutboxRepository.class,
        TicketProcessedEventRepository.class
})
@EnableTransactionManagement
@EnableScheduling
public class TicketServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }
}
