package com.yeyamo_mobile.api.campaign_service.infrastructure.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    
    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public OutboxPublisher(
            OutboxEventRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${yeyamo.kafka.topics.campaign-events:campaign-events}") String topic) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${yeyamo.outbox.publish-delay-ms:1000}")
    @Transactional
    public void publishPending() {
        for (OutboxEventEntity event : repository.findTop100ByPublishedAtIsNullOrderByOccurredAtAsc()) {
            try {
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                event.setPublishedAt(Instant.now());
                event.setLastError(null);
                log.debug("Published event {} to topic {}", event.getId(), topic);
            } catch (Exception exception) {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(abbreviate(exception.getMessage()));
                log.warn("Unable to publish outbox event {} (attempt {})", event.getId(), event.getAttempts());
            }
            repository.save(event);
        }
    }

    private String abbreviate(String message) {
        if (message == null) return "Unknown Kafka publication error";
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
