package com.yeyamo_mobile.api.discovery_service.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Discovery item - can be organic content or sponsored placement
 */
public record DiscoveryItem(
    // Common fields
    String itemType,  // "ORGANIC" or "SPONSORED"
    
    // Organic discovery fields (when itemType=ORGANIC)
    UUID id,
    String sourceId,
    DiscoveryType type,
    String title,
    String description,
    String categoryCode,
    String regionCode,
    String city,
    Double latitude,
    Double longitude,
    String authorId,
    double trendScore,
    boolean active,
    Instant publishedAt,
    Instant updatedAt,
    
    // Sponsored content fields (when itemType=SPONSORED)
    String deliveryId,
    String campaignId,
    String promotedEntityType,
    String promotedEntityId,
    Map<String, Object> creative,
    BigDecimal bidAmount,
    String trackingToken
) {
    
    /**
     * Create organic discovery item from document
     */
    public static DiscoveryItem fromDocument(DiscoveryDocument doc) {
        return new DiscoveryItem(
            "ORGANIC",
            doc.id(), doc.sourceId(), doc.type(), doc.title(), doc.description(),
            doc.categoryCode(), doc.regionCode(), doc.city(), doc.latitude(), doc.longitude(),
            doc.authorId(), doc.trendScore(), doc.active(), doc.publishedAt(), doc.updatedAt(),
            null, null, null, null, null, null, null
        );
    }
    
    /**
     * Create sponsored discovery item
     */
    public static DiscoveryItem sponsored(
            String deliveryId,
            String campaignId,
            String promotedEntityType,
            String promotedEntityId,
            Map<String, Object> creative,
            BigDecimal bidAmount,
            String trackingToken) {
        
        return new DiscoveryItem(
            "SPONSORED",
            null, null, null, null, null, null, null, null, null, null, null, 0, false, null, null,
            deliveryId, campaignId, promotedEntityType, promotedEntityId,
            creative, bidAmount, trackingToken
        );
    }
    
    public boolean isSponsored() {
        return "SPONSORED".equals(itemType);
    }
    
    public boolean isOrganic() {
        return "ORGANIC".equals(itemType);
    }
}
