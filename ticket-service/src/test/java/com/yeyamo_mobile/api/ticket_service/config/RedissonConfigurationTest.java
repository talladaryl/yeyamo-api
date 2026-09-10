package com.yeyamo_mobile.api.ticket_service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.InputStream;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.redisson.config.Config;
import org.redisson.config.EqualJitterDelay;
import org.redisson.config.SingleServerConfig;

class RedissonConfigurationTest {

    @Test
    void parsesRedisson47ConfigurationWithLocalDefaults() throws Exception {
        try (InputStream configuration = getClass().getResourceAsStream("/redisson-config.yaml")) {
            Config config = Config.fromYAML(configuration);
            SingleServerConfig singleServer = config.useSingleServer();

            String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");
            String redisPort = System.getenv().getOrDefault("REDIS_PORT", "6379");
            assertEquals("redis://" + redisHost + ":" + redisPort, singleServer.getAddress());
            assertEquals(10, singleServer.getConnectionPoolSize());
            assertEquals(5, singleServer.getConnectionMinimumIdleSize());
            assertEquals(3000, singleServer.getTimeout());
            assertEquals(3, singleServer.getRetryAttempts());
            EqualJitterDelay retryDelay = assertInstanceOf(EqualJitterDelay.class, singleServer.getRetryDelay());
            assertEquals(Duration.ofSeconds(1), retryDelay.getBaseDelay());
            assertEquals(Duration.ofSeconds(2), retryDelay.getMaxDelay());
            assertEquals(16, config.getThreads());
            assertEquals(32, config.getNettyThreads());
            assertEquals("NIO", config.getTransportMode().name());
            assertEquals("JsonJacksonCodec", config.getCodec().getClass().getSimpleName());
        }
    }
}
