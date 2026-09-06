package com.yeyamo_mobile.api.payment_service.infrastructure.messaging;

import static com.yeyamo_mobile.api.payment_service.application.PaymentCommands.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.*;
import com.yeyamo_mobile.api.payment_service.application.PaymentApplicationService;
import com.yeyamo_mobile.api.payment_service.infrastructure.aggregator.HrSkillsPayService;

@Component
public class PaymentCommandConsumer {
    private final ObjectMapper mapper;
    private final PaymentApplicationService service;
    private final ProcessedEventRepository processed;
    private final HrSkillsPayService hrSkillsPayService;

    public PaymentCommandConsumer(ObjectMapper m, PaymentApplicationService s, ProcessedEventRepository p) {
        this(m, s, p, null);
    }

    @Autowired
    public PaymentCommandConsumer(
            ObjectMapper m,
            PaymentApplicationService s,
            ProcessedEventRepository p,
            @Autowired(required = false) HrSkillsPayService hrSkillsPayService) {
        this.mapper = m;
        this.service = s;
        this.processed = p;
        this.hrSkillsPayService = hrSkillsPayService;
    }

    @KafkaListener(
        topics = "${yeyamo.kafka.topics.payment-commands:payment.commands}",
        groupId = "${spring.kafka.consumer.group-id:payment-service}"
    )
    @Transactional
    public void consume(String raw) throws Exception {
        JsonNode event = mapper.readTree(raw);
        UUID id = UUID.fromString(required(event, "eventId"));
        if (processed.existsById(id)) return;
        if (com.yeyamo.events.EventEnvelopeReader.version(event) != 1) {
            throw new IllegalArgumentException("Unsupported payment command version");
        }
        String type = required(event, "eventType");
        JsonNode p = event.path("payload");
        String correlation = text(event, "correlationId");

        switch (type) {
            case "payment.authorization.requested" -> {
                if (hrSkillsPayService != null && hrSkillsPayService.isEnabled()) {
                    hrSkillsPayService.handleAuthorizationCommand(event, p, correlation);
                } else {
                    service.authorize(new Authorize(
                            uuid(p, "sagaId"),
                            uuid(p, "bookingId"),
                            required(p, "userId"),
                            text(p, "partnerId"),
                            decimal(p, "amount"),
                            required(p, "currency"),
                            required(p, "idempotencyKey"),
                            correlation
                    ));
                }
            }
            case "payment.authorization.cancel.requested" ->
                service.cancelAuthorization(new CancelAuthorization(
                        uuid(p, "sagaId"),
                        uuid(p, "bookingId"),
                        required(p, "idempotencyKey"),
                        correlation
                ));
            case "payment.refund.requested" ->
                service.refund(new Refund(
                        uuid(p, "sagaId"),
                        uuid(p, "bookingId"),
                        text(p, "paymentId"),
                        decimal(p, "amount"),
                        required(p, "currency"),
                        required(p, "idempotencyKey"),
                        correlation
                ));
            default -> {}
        }
        processed.save(new ProcessedEventEntity(id, type));
    }

    private UUID uuid(JsonNode n, String f) { return UUID.fromString(required(n, f)); }
    private BigDecimal decimal(JsonNode n, String f) {
        String v = required(n, f);
        try {
            return new BigDecimal(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(f + " is invalid");
        }
    }
    private String required(JsonNode n, String f) {
        String v = text(n, f);
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " is required");
        return v;
    }
    private String text(JsonNode n, String f) {
        JsonNode v = n.get(f);
        return v == null || v.isNull() ? null : v.asText();
    }
}
