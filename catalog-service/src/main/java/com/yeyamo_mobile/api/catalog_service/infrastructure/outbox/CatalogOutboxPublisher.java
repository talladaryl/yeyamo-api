package com.yeyamo_mobile.api.catalog_service.infrastructure.outbox;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name="yeyamo.outbox.enabled",havingValue="true",matchIfMissing=true)
public class CatalogOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(CatalogOutboxPublisher.class);
    private final CatalogOutboxRepository repository;
    private final KafkaTemplate<String,String> kafka;
    private final String topic;
    public CatalogOutboxPublisher(CatalogOutboxRepository repository, KafkaTemplate<String,String> kafka,
            @Value("${yeyamo.kafka.topics.catalog-events:catalog.events}") String topic) {
        this.repository=repository; this.kafka=kafka; this.topic=topic;
    }
    @Scheduled(fixedDelayString="${yeyamo.outbox.publish-delay-ms:1000}")
    @Transactional public void publishPending() {
        for (CatalogOutboxEvent event : repository.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc()) {
            try {
                kafka.send(topic,event.getAggregateId(),event.getPayload()).get(5,TimeUnit.SECONDS);
                event.setPublishedAt(Instant.now()); event.setLastError(null);
            } catch (Exception ex) {
                event.setAttempts(event.getAttempts()+1);
                String message=ex.getMessage()==null?"Kafka publication failed":ex.getMessage();
                event.setLastError(message.substring(0,Math.min(message.length(),1000)));
                log.warn("Catalog outbox event {} publication failed",event.getId());
            }
            repository.save(event);
        }
    }
}
