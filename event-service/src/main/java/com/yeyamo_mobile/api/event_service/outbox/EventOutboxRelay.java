package com.yeyamo_mobile.api.event_service.outbox;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name="yeyamo.outbox.enabled",havingValue="true",matchIfMissing=true)
public class EventOutboxRelay {
    private static final Logger log=LoggerFactory.getLogger(EventOutboxRelay.class);
    private final EventOutboxRepository repository;
    private final KafkaTemplate<String,String> kafka;
    private final String topic;
    public EventOutboxRelay(EventOutboxRepository repository,KafkaTemplate<String,String> kafka,
            @Value("${yeyamo.kafka.topics.event-events:event.events}") String topic){
        this.repository=repository;this.kafka=kafka;this.topic=topic;
    }
    @Scheduled(fixedDelayString="${yeyamo.outbox.publish-delay-ms:1000}")
    @Transactional
    public void publishPending(){
        for(EventOutboxMessage message:repository.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc()){
            try{
                kafka.send(topic,message.getAggregateId(),message.getPayload()).get(5,TimeUnit.SECONDS);
                message.setPublishedAt(Instant.now());message.setLastError(null);
            }catch(Exception exception){
                message.setAttempts(message.getAttempts()+1);
                String error=exception.getMessage()==null?"Kafka publication failed":exception.getMessage();
                message.setLastError(error.substring(0,Math.min(1000,error.length())));
                log.warn("Event outbox message {} publication failed",message.getId());
            }
            repository.save(message);
        }
    }
}
