package com.yeyamo_mobile.api.discovery_service.infrastructure.messaging;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;
@Configuration public class KafkaErrorConfig {
    @Bean DefaultErrorHandler discoveryErrorHandler(KafkaTemplate<String,String> kafka) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafka,
                (record,error)->new TopicPartition(record.topic()+".DLT",record.partition())),new FixedBackOff(1000,3));
    }
}
