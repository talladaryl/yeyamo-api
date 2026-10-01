package com.yeyamo_mobile.api.messaging_service.infrastructure.messaging;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.messaging_service.application.MessagingApplicationService;
import com.yeyamo_mobile.api.messaging_service.application.MessagingDtos.CreateConversation;
import com.yeyamo_mobile.api.messaging_service.application.port.MessagingEventPort;
import com.yeyamo_mobile.api.messaging_service.domain.ConversationType;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.OutingGroupLinkEntity;
import com.yeyamo_mobile.api.messaging_service.infrastructure.persistence.OutingGroupLinkRepository;

/** Creates exactly one owner-only group when a public outing is published. */
@Component
public class OutingGroupConsumer {
    private static final Logger log = LoggerFactory.getLogger(OutingGroupConsumer.class);
    private final ObjectMapper mapper;
    private final MessagingApplicationService conversations;
    private final OutingGroupLinkRepository links;
    private final MessagingEventPort events;

    public OutingGroupConsumer(ObjectMapper mapper, MessagingApplicationService conversations,
            OutingGroupLinkRepository links, MessagingEventPort events) {
        this.mapper = mapper;
        this.conversations = conversations;
        this.links = links;
        this.events = events;
    }

    @KafkaListener(topics = "${yeyamo.kafka.topics.event-events:event.events}",
            groupId = "${spring.kafka.consumer.group-id:messaging-service}-outing-groups")
    @Transactional
    public void consume(String raw) throws Exception {
        JsonNode event = mapper.readTree(raw);
        if (!"event-service".equals(text(event, "producer")) || !"event.published".equals(text(event, "eventType"))) return;
        JsonNode payload = event.path("payload");
        if (!"PUBLIC".equals(text(payload, "visibility"))) return;

        UUID outingId = UUID.fromString(required(payload, "eventId"));
        String ownerId = required(payload, "organizerUserId");
        String title = required(payload, "title");
        String correlationId = text(event, "correlationId");

        OutingGroupLinkEntity existing = links.findById(outingId).orElse(null);
        if (existing != null) {
            log.info("event=OUTING_GROUP_ALREADY_EXISTS outingId={} groupId={} correlationId={}", outingId, existing.getConversationId(), correlationId);
            return;
        }

        log.info("event=OUTING_GROUP_CREATE_START outingId={} correlationId={}", outingId, correlationId);
        var group = conversations.create(ownerId, new CreateConversation(ConversationType.GROUP, groupTitle(title), Set.of()), correlationId);
        links.save(OutingGroupLinkEntity.create(outingId, group.id(), ownerId));
        events.publish("messaging.outing.group.created", outingId.toString(), ownerId, correlationId, Set.of(ownerId),
                Map.of("outingId", outingId.toString(), "groupId", group.id().toString()), group);
        log.info("event=OUTING_GROUP_CREATED outingId={} groupId={} correlationId={}", outingId, group.id(), correlationId);
    }

    private static String required(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }
    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
    private static String groupTitle(String title) {
        return title.length() <= 120 ? title : title.substring(0, 120);
    }
}
