package com.yeyamo_mobile.api.ticket_service;

import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
class TicketServiceTestConfiguration {

    @Bean
    RedissonClient redissonClient() {
        return Mockito.mock(RedissonClient.class);
    }
}
