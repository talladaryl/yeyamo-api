package com.yeyamo_mobile.api.content_service.application;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.yeyamo_mobile.api.content_service.infrastructure.persistence.ContentProcessedEventReceipt;
import com.yeyamo_mobile.api.content_service.infrastructure.persistence.ContentProcessedEventReceiptRepository;

/**
 * Coordinates independent Feed and Story transactions. A successful target is
 * never rolled back because the other target needs a Kafka retry.
 */
@Service
public class EventSocialDistributionService {
    private final EventSocialDistributionTargetService targets;
    private final ContentProcessedEventReceiptRepository receipts;

    public EventSocialDistributionService(
            EventSocialDistributionTargetService targets,
            ContentProcessedEventReceiptRepository receipts) {
        this.targets = targets;
        this.receipts = receipts;
    }

    public void distribute(EventPublishedSocialCommand command) {
        if (!command.hasRequestedTarget()) {
            recordReceipt(command);
            return;
        }

        if (command.publishToFeed()) {
            processTarget(command, "FEED");
        }
        if (command.publishToStory()) {
            processTarget(command, "STORY");
        }
        recordReceipt(command);
    }

    private void processTarget(EventPublishedSocialCommand command, String target) {
        try {
            if ("FEED".equals(target)) {
                targets.processFeed(command);
            } else {
                targets.processStory(command);
            }
        } catch (RuntimeException failure) {
            targets.markFailed(command, target, failure);
            throw failure;
        }
    }

    private void recordReceipt(EventPublishedSocialCommand command) {
        if (receipts.existsById(command.sourceEventId())) {
            return;
        }
        ContentProcessedEventReceipt receipt = new ContentProcessedEventReceipt();
        receipt.setEventId(command.sourceEventId());
        receipt.setEventType("event.published");
        receipt.setProcessedAt(Instant.now());
        receipts.save(receipt);
    }
}
