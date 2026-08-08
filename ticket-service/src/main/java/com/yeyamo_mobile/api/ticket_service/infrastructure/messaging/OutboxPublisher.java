package com.yeyamo_mobile.api.ticket_service.infrastructure.messaging;

import com.yeyamo_mobile.api.ticket_service.domain.model.OutboxEvent;
import com.yeyamo_mobile.api.ticket_service.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Publishes events from outbox table to Kafka.
 * Runs periodically to ensure eventual consistency.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    
    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    private static final String TOPIC_PREFIX = "yeyamo.ticket.";
    
    /**
     * Publish unpublished events every 5 seconds
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> unpublished = outboxRepository.findUnpublishedEvents();
        
        if (unpublished.isEmpty()) {
            return;
        }
        
        log.debug("Publishing {} outbox events", unpublished.size());
        
        for (OutboxEvent event : unpublished) {
            try {
                publishEvent(event);
                event.markAsPublished();
                outboxRepository.save(event);
                
            } catch (Exception e) {
                log.error("Failed to publish event: {}", event.getId(), e);
                // Event will be retried on next scheduled run
            }
        }
    }
    
    /**
     * Publish single event to Kafka
     */
    private void publishEvent(OutboxEvent event) {
        String topic = TOPIC_PREFIX + event.getEventType().replace('.', '-');
        String key = event.getAggregateId();
        String payload = event.getPayload();
        
        kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send event to Kafka: topic={}, key={}", 
                                 topic, key, ex);
                    } else {
                        log.debug("Published event to Kafka: topic={}, key={}, partition={}, offset={}", 
                                 topic, key, 
                                 result.getRecordMetadata().partition(),
                                 result.getRecordMetadata().offset());
                    }
                });
    }
}
