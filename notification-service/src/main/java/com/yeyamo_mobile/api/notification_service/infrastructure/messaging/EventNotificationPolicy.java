package com.yeyamo_mobile.api.notification_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.notification_service.application.NotificationIntent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Maps versioned domain events to the user-facing notification vocabulary. */
@Component
public class EventNotificationPolicy {

    private final ObjectMapper mapper;

    public EventNotificationPolicy(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<NotificationIntent> map(JsonNode event) {
        String eventType = required(event, "eventType");
        if ("messaging.message.sent".equals(eventType)) {
            return messaging(event);
        }
        String notificationType = notificationType(eventType);
        if (notificationType == null) {
            return List.of();
        }
        UUID eventId = UUID.fromString(required(event, "eventId"));
        if (event.path("eventVersion").asInt(0) != 1) {
            throw new IllegalArgumentException("Unsupported event version");
        }
        JsonNode payload = event.path("payload");
        String recipient = recipientFor(eventType, payload);
        if (recipient == null) {
            return List.of();
        }
        return List.of(new NotificationIntent(eventId, notificationType, recipient,
                text(payload, "email"), variables(eventType, notificationType, payload), serialize(payload)));
    }

    private List<NotificationIntent> messaging(JsonNode event) {
        UUID eventId = UUID.fromString(required(event, "eventId"));
        if (event.path("eventVersion").asInt(0) != 1) {
            throw new IllegalArgumentException("Unsupported event version");
        }
        JsonNode payload = event.path("payload");
        JsonNode recipients = payload.path("recipientIds");
        if (!recipients.isArray()) {
            throw new IllegalArgumentException("recipientIds is required");
        }
        Map<String, String> variables = variables("messaging.message.sent", "messaging.message.sent", payload);
        List<NotificationIntent> result = new ArrayList<>();
        for (JsonNode recipient : recipients) {
            if (!recipient.asText().isBlank()) {
                result.add(new NotificationIntent(eventId, "messaging.message.sent", recipient.asText(), null,
                        variables, serialize(payload)));
            }
        }
        return result;
    }

    private String notificationType(String eventType) {
        return switch (eventType) {
            case "user.created", "user.role_added", "user.password_changed",
                    "partner.submitted", "partner.approved", "partner.rejected", "partner.needs_info",
                    "partner.requires_changes", "moderation.report.approved", "moderation.report.rejected" -> eventType;
            case "CultureContributionApproved", "CultureContentVerified" -> "CULTURE_CONTRIBUTION_APPROVED";
            case "CultureContributionRejected", "CultureContentRejected" -> "CULTURE_CONTRIBUTION_REJECTED";
            case "CultureTranslationVerified", "TranslationVerified" -> "TRANSLATION_VERIFIED";
            case "CultureChallengeJoined", "CultureChallengeStarted", "CultureChallengeSubmitted" -> "CHALLENGE_STARTED";
            case "CultureChallengeCompleted", "CultureChallengeResult", "CultureChallenge.result" -> "CHALLENGE_RESULT";
            case "ArtworkLiked" -> "ARTWORK_LIKED";
            case "ArtworkSold" -> "ARTWORK_SOLD";
            case "ArtworkOrderCreated" -> "ARTWORK_ORDER_CREATED";
            case "ArtisanFollowed" -> "ARTISAN_FOLLOWED";
            case "ArtworkAuthenticityVerified", "ArtisanVerified" -> "AUTHENTICITY_VERIFIED";
            default -> eventType.startsWith("ArtworkOrder") ? "ARTWORK_ORDER_UPDATED" : null;
        };
    }

    private String recipientFor(String eventType, JsonNode payload) {
        return switch (eventType) {
            case "user.created", "user.role_added", "user.password_changed" -> required(payload, "userId");
            case "partner.submitted" -> required(payload, "requesterId");
            case "partner.approved", "partner.rejected", "partner.needs_info", "partner.requires_changes" -> required(payload, "ownerUserId");
            case "moderation.report.approved", "moderation.report.rejected" -> required(payload, "reporterId");
            case "CultureContributionApproved", "CultureContributionRejected", "CultureContentVerified", "CultureContentRejected" -> requiredAny(payload, "contributorId", "authorId", "createdBy");
            case "CultureTranslationVerified", "TranslationVerified" -> requiredAny(payload, "translatorId", "proposerId", "authorId");
            case "CultureChallengeJoined", "CultureChallengeStarted", "CultureChallengeSubmitted", "CultureChallengeCompleted", "CultureChallengeResult", "CultureChallenge.result" -> required(payload, "userId");
            case "ArtworkLiked" -> first(payload, "artisanUserId", "artisanId", "artisanPartnerId", "ownerUserId");
            case "ArtworkSold", "ArtworkOrderCreated" -> requiredAny(payload, "artisanUserId", "artisanId", "artisanPartnerId", "ownerUserId");
            case "ArtworkAuthenticityVerified", "ArtisanVerified" -> requiredAny(payload, "artisanUserId", "artisanId", "artisanPartnerId", "partnerId", "claimantId");
            case "ArtisanFollowed" -> requiredAny(payload, "artisanUserId", "artisanId", "artisanPartnerId", "targetId");
            default -> requiredAny(payload, "buyerUserId", "buyerId", "artisanUserId", "artisanId", "artisanPartnerId");
        };
    }

    private Map<String, String> variables(String eventType, String notificationType, JsonNode payload) {
        Map<String, String> variables = new HashMap<>();
        payload.fields().forEachRemaining(field -> {
            if (field.getValue().isValueNode()) {
                variables.put(field.getKey(), field.getValue().asText());
            }
        });
        variables.put("eventType", eventType);
        variables.put("notificationType", notificationType);
        variables.putIfAbsent("message", notificationType);
        return variables;
    }

    private String serialize(JsonNode payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot serialize notification data", exception);
        }
    }

    private String requiredAny(JsonNode node, String... fields) {
        String value = first(node, fields);
        if (value != null) return value;
        throw new IllegalArgumentException(String.join(" or ", fields) + " is required");
    }

    private String first(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private String required(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
