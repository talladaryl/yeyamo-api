package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaErrorConfig {
    @Bean
    DefaultErrorHandler adsErrorHandler(KafkaTemplate<Object, Object> kafka) {
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(kafka,
                (record, error) -> new TopicPartition(
                    record.topic() + ".DLT", record.partition()));
        ExponentialBackOff backoff = new ExponentialBackOff(500, 2);
        backoff.setMaxInterval(5_000);
        backoff.setMaxElapsedTime(15_000);
        return new DefaultErrorHandler(recoverer, backoff);
    }
}
