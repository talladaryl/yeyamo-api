package com.yeyamo_mobile.api.catalog_service.infrastructure.messaging;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;
@Configuration
public class KafkaErrorConfig {
    @Bean DefaultErrorHandler catalogErrorHandler(KafkaTemplate<String,String> template){
        DeadLetterPublishingRecoverer recoverer=new DeadLetterPublishingRecoverer(template,
                (record,error)->new TopicPartition(record.topic()+".DLT",record.partition()));
        return new DefaultErrorHandler(recoverer,new FixedBackOff(1000,3));
    }
}
