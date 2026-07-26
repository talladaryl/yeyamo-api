package com.yeyamo_mobile.api.ticket_service.infrastructure.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisher.class);
    
    private final SpringOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public OutboxPublisher(
            SpringOutboxRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @Scheduled(fixedDelay = 5000) // Every 5 seconds
    @Transactional
    public void publishPendingEvents() {
        List<TicketOutboxEntity> pending = outboxRepository.findPendingEventsBatch();
        
        if (pending.isEmpty()) {
            return;
        }
        
        logger.debug("Publishing {} pending outbox events", pending.size());
        
        for (TicketOutboxEntity event : pending) {
            try {
                String topic = mapEventTypeToTopic(event.getEventType());
                
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload()).get();
                markAsPublished(event);
                    
            } catch (Exception e) {
                logger.error("Error publishing outbox event: " + event.getId(), e);
            }
        }
    }
    
    @Transactional
    private void markAsPublished(TicketOutboxEntity event) {
        event.setStatus("PUBLISHED");
        event.setPublishedAt(Instant.now());
        outboxRepository.save(event);
    }
    
    private String mapEventTypeToTopic(String eventType) {
        return switch (eventType) {
            case "payment.authorization.requested" -> "payment.commands";
            case "TicketsIssued", "TicketCancelled" -> "ticket.events";
            case "TicketScanned" -> "ticket.scan.events";
            default -> "ticket.events";
        };
    }
}
