package com.yeyamo_mobile.api.auth_service.event;

import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.auth_service.enums.AccountType;
import com.yeyamo_mobile.api.auth_service.enums.Roles;
import com.yeyamo_mobile.api.auth_service.models.Role;
import com.yeyamo_mobile.api.auth_service.models.User;
import com.yeyamo_mobile.api.auth_service.repository.RoleRepository;
import com.yeyamo_mobile.api.auth_service.repository.UserRepository;

@Component
public class PartnerApprovedConsumer {
    private final ObjectMapper mapper;
    private final UserRepository users;
    private final RoleRepository roles;
    private final AuthProcessedEventRepository processed;
    private final AuthEventOutbox outbox;

    public PartnerApprovedConsumer(ObjectMapper mapper, UserRepository users, RoleRepository roles,
            AuthProcessedEventRepository processed, AuthEventOutbox outbox) {
        this.mapper = mapper;
        this.users = users;
        this.roles = roles;
        this.processed = processed;
        this.outbox = outbox;
    }

    @KafkaListener(topics = "${yeyamo.kafka.topics.partner-events:partner-events}",
            groupId = "${spring.kafka.consumer.group-id:auth-service}")
    @Transactional
    public void consume(String raw) throws Exception {
        JsonNode event = mapper.readTree(raw);
        if (!"partner.approved".equals(text(event, "eventType"))) return;
        UUID eventId = UUID.fromString(required(event, "eventId"));
        if (processed.existsById(eventId)) return;
        JsonNode payload = event.path("payload");
        long userId = Long.parseLong(required(payload, "ownerUserId"));
        User user = users.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Partner owner does not exist in auth-service"));
        Role role = roles.findByCode(Roles.PARTNER)
                .orElseThrow(() -> new IllegalStateException("PARTNER role is not seeded"));
        user.setAccountType(AccountType.PARTNER);
        boolean roleAdded = user.getRoles().add(role);
        users.save(user);
        if (roleAdded) outbox.roleAdded(user, Roles.PARTNER.name(), text(event, "correlationId"));
        processed.save(new AuthProcessedEvent(eventId, "partner.approved"));
    }

    private String required(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
