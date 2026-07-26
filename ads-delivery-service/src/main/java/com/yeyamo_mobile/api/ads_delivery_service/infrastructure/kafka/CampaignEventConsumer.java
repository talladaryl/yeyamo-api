package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.kafka;

import com.fasterxml.jackson.databind.*;
import com.yeyamo_mobile.api.ads_delivery_service.domain.model.*;
import com.yeyamo_mobile.api.ads_delivery_service.domain.port.CampaignProjectionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Component
@ConditionalOnProperty(name = "yeyamo.kafka.enabled",
    havingValue = "true", matchIfMissing = true)
public class CampaignEventConsumer {
    private final CampaignProjectionRepository campaigns;
    private final ProcessedCampaignEventRepository processed;
    private final ObjectMapper json;

    public CampaignEventConsumer(CampaignProjectionRepository campaigns,
            ProcessedCampaignEventRepository processed, ObjectMapper json) {
        this.campaigns = campaigns;
        this.processed = processed;
        this.json = json;
    }

    @KafkaListener(topics = "${yeyamo.kafka.topics.campaign-events:campaign-events}",
        groupId = "${spring.kafka.consumer.group-id:ads-delivery-service}")
    @Transactional
    public void handleCampaignEvent(String raw) throws Exception {
        JsonNode root = json.readTree(raw);
        UUID eventId = UUID.fromString(required(root, "eventId"));
        if (processed.existsById(eventId)) return;
        if (!validVersion(root.path("eventVersion"))) {
            throw new IllegalArgumentException("Unsupported campaign event version");
        }
        String eventType = required(root, "eventType");
        JsonNode payload = root.path("payload");
        if (!payload.isObject()) throw new IllegalArgumentException("payload is required");

        switch (eventType) {
            case "campaign.activated", "campaign.resumed", "campaign.updated" ->
                activate(payload, root.path("occurredAt").asText());
            case "campaign.paused", "campaign.cancelled",
                 "campaign.budget.exhausted", "campaign.completed" ->
                campaigns.deactivate(required(payload, "campaignId"));
            default -> { }
        }
        processed.save(new ProcessedCampaignEvent(eventId, eventType));
    }

    private void activate(JsonNode payload, String occurredAt) throws Exception {
        JsonNode target = payload.path("targetConfiguration");
        JsonNode creative = payload.path("creativeConfiguration");
        BigDecimal dailyBudget = decimal(payload, "dailyBudget",
            decimal(payload, "totalBudget", BigDecimal.ZERO));
        CampaignProjection projection = CampaignProjection.builder()
            .campaignId(required(payload, "campaignId"))
            .partnerId(required(payload, "partnerId"))
            .name(required(payload, "name"))
            .objective(required(payload, "objective"))
            .promotedEntityType(PromotedEntityType.valueOf(
                required(payload, "promotedEntityType")))
            .promotedEntityId(required(payload, "promotedEntityId"))
            .billingModel(required(payload, "billingModel"))
            .bidAmount(decimal(payload, "bidAmount", BigDecimal.ONE))
            .totalBudget(decimal(payload, "totalBudget", BigDecimal.ZERO))
            .dailyBudget(dailyBudget)
            .spentAmount(decimal(payload, "spentAmount", BigDecimal.ZERO))
            .currency(required(payload, "currency"))
            .startAt(Instant.parse(required(payload, "startAt")))
            .endAt(Instant.parse(required(payload, "endAt")))
            .eligiblePlacements(enumList(payload.path("eligiblePlacements")))
            .targetCountries(strings(target.path("countryCodes")))
            .targetRegions(strings(target.path("regionIds")))
            .targetCities(strings(target.path("cityIds")))
            .targetInterests(strings(target.path("interestIds")))
            .targetCategories(strings(target.path("categoryIds")))
            .minAge(text(target, "minimumAge"))
            .maxAge(text(target, "maximumAge"))
            .targetLanguages(strings(target.path("languageCodes")))
            .creativeJson(creative.isObject() ? json.writeValueAsString(creative) : "{}")
            .qualityScore(50)
            .createdAt(Instant.parse(occurredAt))
            .build();
        campaigns.save(projection);
    }

    private boolean validVersion(JsonNode version) {
        return version.isInt() && version.asInt() == 1
            || version.isTextual()
                && ("1".equals(version.asText()) || "1.0".equals(version.asText()));
    }

    private String required(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private BigDecimal decimal(JsonNode node, String field, BigDecimal fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.decimalValue();
    }

    private List<String> strings(JsonNode array) {
        if (!array.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asText()));
        return values;
    }

    private List<PlacementType> enumList(JsonNode array) {
        return strings(array).stream().map(PlacementType::valueOf).toList();
    }
}
