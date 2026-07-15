package com.yeyamo_mobile.api.analytics_service.config;
import org.apache.kafka.common.TopicPartition;import org.springframework.context.annotation.*;import org.springframework.kafka.core.KafkaTemplate;import org.springframework.kafka.listener.*;import org.springframework.util.backoff.ExponentialBackOff;
@Configuration public class KafkaErrorConfig{
 @Bean DefaultErrorHandler analyticsErrorHandler(KafkaTemplate<String,String> template){
  DeadLetterPublishingRecoverer recoverer=new DeadLetterPublishingRecoverer(template,(record,error)->new TopicPartition(record.topic()+".DLT",record.partition()));
  ExponentialBackOff backoff=new ExponentialBackOff(500,2.0);backoff.setMaxInterval(5000);backoff.setMaxElapsedTime(12000);
  return new DefaultErrorHandler(recoverer,backoff);
 }
}
