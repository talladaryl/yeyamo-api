package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class AdsOutboxPublisher {
    private final AdsOutboxRepository repository;
    private final KafkaTemplate<Object, Object> kafka;
    private final String topic;

    public AdsOutboxPublisher(AdsOutboxRepository repository,
            KafkaTemplate<Object, Object> kafka,
            @Value("${yeyamo.kafka.topics.ad-events:ad-events}") String topic) {
        this.repository = repository;
        this.kafka = kafka;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${yeyamo.ads.outbox-delay-ms:1000}")
    @Transactional
    public void publish() {
        for (AdsOutboxEvent event :
                repository.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc()) {
            try {
                kafka.send(topic, event.aggregateId, event.payload)
                    .get(5, TimeUnit.SECONDS);
                event.publishedAt = Instant.now();
                event.lastError = null;
            } catch (Exception exception) {
                event.attempts++;
                String message = String.valueOf(exception.getMessage());
                event.lastError = message.substring(0, Math.min(1000, message.length()));
            }
            repository.save(event);
        }
    }
}
