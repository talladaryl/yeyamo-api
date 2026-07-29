package com.yeyamo_mobile.api.auth_service.event;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.auth_service.models.AdminUserProfileProjection;
import com.yeyamo_mobile.api.auth_service.repository.AdminUserProfileProjectionRepository;

@Component
public class UserProfileProjectionConsumer {
    private static final Set<String> SUPPORTED = Set.of("profile.created", "profile.updated",
            "profile.preferences_updated", "profile.deleted");
    private final ObjectMapper objectMapper;
    private final AdminUserProfileProjectionRepository repository;

    public UserProfileProjectionConsumer(ObjectMapper objectMapper, AdminUserProfileProjectionRepository repository) {
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    @KafkaListener(topics = "${yeyamo.kafka.topics.user-events:user-events}", groupId = "${spring.kafka.consumer.group-id:auth-service}-admin-projection")
    @Transactional
    public void consume(String raw) throws Exception {
        JsonNode event = objectMapper.readTree(raw);
        if (!SUPPORTED.contains(event.path("eventType").asText())) return;
        JsonNode payload = event.path("payload");
        long authUserId = Long.parseLong(payload.path("authUserId").asText());
        if ("profile.deleted".equals(event.path("eventType").asText())) {
            repository.deleteById(authUserId);
            return;
        }
        AdminUserProfileProjection projection = repository.findById(authUserId).orElseGet(AdminUserProfileProjection::new);
        projection.setAuthUserId(authUserId);
        if (payload.hasNonNull("profileId")) projection.setProfileId(UUID.fromString(payload.path("profileId").asText()));
        if (payload.has("displayName")) projection.setDisplayName(payload.path("displayName").asText(null));
        if (payload.has("avatarUrl")) projection.setAvatarUrl(payload.path("avatarUrl").asText(null));
        if (payload.hasNonNull("preferredRegionId")) projection.setRegionId(payload.path("preferredRegionId").asLong());
        projection.setUpdatedAt(Instant.now());
        repository.save(projection);
    }
}
