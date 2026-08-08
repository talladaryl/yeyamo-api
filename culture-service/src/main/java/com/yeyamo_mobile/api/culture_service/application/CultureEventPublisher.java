package com.yeyamo_mobile.api.culture_service.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo.events.EventEnvelope;
import com.yeyamo_mobile.api.culture_service.infrastructure.outbox.CultureOutbox;
import com.yeyamo_mobile.api.culture_service.infrastructure.outbox.CultureOutboxRepository;
import com.yeyamo_mobile.api.culture_service.infrastructure.persistence.CultureAuditEvent;
import com.yeyamo_mobile.api.culture_service.infrastructure.persistence.CultureRepositories.Audits;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CultureEventPublisher {
    private final CultureOutboxRepository outbox;
    private final ObjectMapper mapper;
    private final Audits audits;

    @Autowired
    public CultureEventPublisher(CultureOutboxRepository outbox, ObjectMapper mapper, Audits audits) {
        this.outbox = outbox;
        this.mapper = mapper;
        this.audits = audits;
    }

    /** Kept for focused unit tests that do not need a JPA audit repository. */
    CultureEventPublisher(CultureOutboxRepository outbox, ObjectMapper mapper) {
        this.outbox = outbox;
        this.mapper = mapper;
        this.audits = null;
    }

    public void publish(String type, UUID aggregateId, Object payload) {
        try {
            EventEnvelope<Object> envelope = EventEnvelope.create(type, "culture-service", MDC.get("correlationId"),
                    aggregateId.toString(), payload);
            outbox.save(CultureOutbox.create(envelope.eventId(), aggregateId.toString(), type,
                    mapper.writeValueAsString(envelope), envelope.occurredAt()));
            if (audits != null) {
                audits.save(CultureAuditEvent.recorded(aggregateId.toString(), type, envelope.correlationId()));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Culture event serialization failed", exception);
        }
    }
}
