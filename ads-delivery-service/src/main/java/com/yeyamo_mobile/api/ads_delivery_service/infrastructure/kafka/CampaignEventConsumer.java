package com.yeyamo_mobile.api.ads_delivery_service.infrastructure.kafka;

import com.yeyamo_mobile.api.ads_delivery_service.domain.model.CampaignProjection;
import com.yeyamo_mobile.api.ads_delivery_service.domain.model.PlacementType;
import com.yeyamo_mobile.api.ads_delivery_service.domain.model.PromotedEntityType;
import com.yeyamo_mobile.api.ads_delivery_service.domain.port.CampaignProjectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "yeyamo.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class CampaignEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(CampaignEventConsumer.class);
    
    private final CampaignProjectionRepository campaignRepository;

    public CampaignEventConsumer(CampaignProjectionRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @KafkaListener(topics = "${yeyamo.kafka.topics.campaign-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleCampaignEvent(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            String campaignId = (String) event.get("campaignId");

            logger.info("Received campaign event: {} for campaign: {}", eventType, campaignId);

            switch (eventType) {
                case "CampaignActivated":
                    handleCampaignActivated(event);
                    break;
                case "CampaignPaused":
                case "CampaignCancelled":
                case "CampaignBudgetExhausted":
                case "CampaignCompleted":
                    handleCampaignDeactivated(campaignId);
                    break;
                default:
                    logger.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            logger.error("Error processing campaign event", e);
        }
    }

    private void handleCampaignActivated(Map<String, Object> event) {
        String campaignId = (String) event.get("campaignId");
        
        // Build projection from event
        CampaignProjection projection = CampaignProjection.builder()
            .campaignId(campaignId)
            .partnerId((String) event.get("partnerId"))
            .name((String) event.get("name"))
            .objective((String) event.get("objective"))
            .promotedEntityType(PromotedEntityType.valueOf((String) event.get("promotedEntityType")))
            .promotedEntityId((String) event.get("promotedEntityId"))
            .billingModel((String) event.get("billingModel"))
            .bidAmount(new BigDecimal((String) event.get("bidAmount")))
            .totalBudget(new BigDecimal((String) event.get("totalBudget")))
            .dailyBudget(new BigDecimal((String) event.get("dailyBudget")))
            .spentAmount(BigDecimal.ZERO)
            .currency((String) event.get("currency"))
            .startAt(Instant.parse((String) event.get("startAt")))
            .endAt(Instant.parse((String) event.get("endAt")))
            .eligiblePlacements(parsePlacements(event))
            .targetCountries(parseStringList(event, "targetCountries"))
            .targetRegions(parseStringList(event, "targetRegions"))
            .targetCities(parseStringList(event, "targetCities"))
            .targetInterests(parseStringList(event, "targetInterests"))
            .targetCategories(parseStringList(event, "targetCategories"))
            .targetLanguages(parseStringList(event, "targetLanguages"))
            .creativeJson((String) event.get("creativeJson"))
            .qualityScore(50) // Default quality score
            .createdAt(Instant.now())
            .build();

        campaignRepository.save(projection);
        
        logger.info("Saved campaign projection for campaign: {}", campaignId);
    }

    private void handleCampaignDeactivated(String campaignId) {
        // In a real implementation, we would update the status or delete the projection
        // For now, just log
        logger.info("Campaign deactivated: {}", campaignId);
    }

    @SuppressWarnings("unchecked")
    private List<PlacementType> parsePlacements(Map<String, Object> event) {
        Object placements = event.get("eligiblePlacements");
        if (placements instanceof List) {
            return ((List<String>) placements).stream()
                .map(PlacementType::valueOf)
                .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(Map<String, Object> event, String key) {
        Object value = event.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return List.of();
    }
}
