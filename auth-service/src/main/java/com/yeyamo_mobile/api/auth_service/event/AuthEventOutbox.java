package com.yeyamo_mobile.api.auth_service.event;

import java.time.Instant;
import java.util.*;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.auth_service.models.User;

@Component
public class AuthEventOutbox {
    private final AuthOutboxRepository repo;
    private final ObjectMapper mapper;

    public AuthEventOutbox(AuthOutboxRepository r, ObjectMapper m) {
        repo = r;
        mapper = m;
    }

    public void userCreated(User user, String correlationId) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("userId", String.valueOf(user.getId()));
        p.put("email", user.getEmail());
        p.put("phone", user.getPhone());
        p.put("status", user.getStatus().name());
        // Add geographic fields
        p.put("countryCode", user.getCountryCode());
        p.put("cityId", user.getCityId() != null ? user.getCityId().toString() : null);
        p.put("preferredLanguageCode", user.getPreferredLanguageCode());
        p.put("timezone", user.getTimezone());
        append("user.created", String.valueOf(user.getId()), String.valueOf(user.getId()), correlationId, p);
    }

    public void roleAdded(User user, String role, String correlationId) {
        append("user.role_added", String.valueOf(user.getId()), "partner-service", correlationId,
                Map.of("userId", String.valueOf(user.getId()), "role", role));
    }

    public void adminChanged(User user, String eventType, String actor, String reason, String correlationId,
            Map<String, Object> changes) {
        Map<String, Object> p = new LinkedHashMap<>(changes);
        p.put("userId", String.valueOf(user.getId()));
        p.put("reason", reason);
        append(eventType, String.valueOf(user.getId()), actor, correlationId, p);
    }

    public void adminExport(String actor, String correlationId, Map<String, Object> filters) {
        append("admin.platform_users_exported", "platform-users", actor, correlationId, filters);
    }

    public void passwordChanged(User user, String correlationId) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("userId", String.valueOf(user.getId()));
        p.put("email", user.getEmail());
        p.put("changedAt", Instant.now());
        append("user.password_changed", String.valueOf(user.getId()), String.valueOf(user.getId()), correlationId, p);
    }

    private void append(String type, String aggregate, String actor, String correlation, Map<String, Object> payload) {
        try {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("eventId", id);
            e.put("eventType", type);
            e.put("eventVersion", 1);
            e.put("occurredAt", now);
            e.put("producer", "auth-service");
            e.put("correlationId", correlation);
            e.put("actorId", actor);
            e.put("payload", payload);
            AuthOutboxEvent row = new AuthOutboxEvent();
            row.setId(id);
            row.setAggregateId(aggregate);
            row.setEventType(type);
            row.setPayload(mapper.writeValueAsString(e));
            row.setOccurredAt(now);
            repo.save(row);
        } catch (Exception x) {
            throw new IllegalStateException("Unable to create auth event", x);
        }
    }
}
