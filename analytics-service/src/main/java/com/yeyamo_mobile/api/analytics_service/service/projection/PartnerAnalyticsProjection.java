package com.yeyamo_mobile.api.analytics_service.service.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.analytics_service.event.AnalyticsDomainEvent;
import com.yeyamo_mobile.api.analytics_service.models.CatalogAssetDimension;
import com.yeyamo_mobile.api.analytics_service.models.ContentDimension;
import com.yeyamo_mobile.api.analytics_service.models.PartnerAnalytics;
import com.yeyamo_mobile.api.analytics_service.models.PartnerDimension;
import com.yeyamo_mobile.api.analytics_service.repository.CatalogAssetDimensionRepository;
import com.yeyamo_mobile.api.analytics_service.repository.ContentDimensionRepository;
import com.yeyamo_mobile.api.analytics_service.repository.PartnerAnalyticsRepository;
import com.yeyamo_mobile.api.analytics_service.repository.PartnerDimensionRepository;

@Component
public class PartnerAnalyticsProjection implements AnalyticsProjection {
    private final PartnerAnalyticsRepository analytics;
    private final PartnerDimensionRepository partners;
    private final CatalogAssetDimensionRepository assets;
    private final ContentDimensionRepository contents;

    public PartnerAnalyticsProjection(PartnerAnalyticsRepository analytics, PartnerDimensionRepository partners,
            CatalogAssetDimensionRepository assets, ContentDimensionRepository contents) {
        this.analytics = analytics;
        this.partners = partners;
        this.assets = assets;
        this.contents = contents;
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public boolean supports(AnalyticsDomainEvent event) {
        String type = event.eventType();
        return type.startsWith("interaction.") || type.startsWith("booking.")
                || type.startsWith("content.post.");
    }

    @Override
    public void project(AnalyticsDomainEvent event) {
        UUID partnerId = resolvePartner(event);
        if (partnerId == null) {
            return;
        }
        String type = event.eventType();
        long views = type.equals("interaction.post.viewed") ? 1 : 0;
        long bookings = type.equals("booking.confirmed") ? 1
                : type.equals("booking.cancelled") ? -1 : 0;
        long reviews = type.equals("interaction.review.created") ? 1 : 0;
        BigDecimal rating = reviews == 0 ? BigDecimal.ZERO : ProjectionSupport.decimal(event.payload(), "rating");
        BigDecimal amount = type.equals("booking.confirmed") ? ProjectionSupport.decimal(event.payload(), "amount")
                : type.equals("booking.cancelled") ? ProjectionSupport.decimal(event.payload(), "amount").negate()
                        : BigDecimal.ZERO;
        long checkIns = type.equals("interaction.checkin.created") ? 1 : 0;
        if (views + bookings + reviews + checkIns == 0 && amount.signum() == 0) {
            return;
        }
        LocalDate date = ProjectionSupport.date(event);
        UUID id = ProjectionSupport.id("partner-analytics", partnerId, date);
        PartnerAnalytics projection = analytics.findById(id).orElseGet(PartnerAnalytics::new);
        projection.setId(id);
        projection.setPartnerId(partnerId);
        projection.setStatDate(date);
        if (projection.apply(event.eventId(), views, bookings, reviews, rating, amount, checkIns)) {
            analytics.save(projection);
        }
    }

    private UUID resolvePartner(AnalyticsDomainEvent event) {
        UUID direct = ProjectionSupport.uuid(event.payload(), "partnerId");
        if (direct != null) {
            return direct;
        }
        UUID assetId = resolveAsset(event);
        CatalogAssetDimension asset = assetId == null ? null : assets.findById(assetId).orElse(null);
        if (asset == null || asset.getOwnerId() == null) {
            return null;
        }
        PartnerDimension partner = partners.findByOwnerUserId(asset.getOwnerId()).orElse(null);
        return partner == null ? null : partner.getPartnerId();
    }

    private UUID resolveAsset(AnalyticsDomainEvent event) {
        UUID direct = ProjectionSupport.uuid(event.payload(), "catalogAssetId", "placeId", "activityId", "assetId");
        if (direct != null) {
            return direct;
        }
        UUID postId = ProjectionSupport.uuid(event.payload(), "postId");
        ContentDimension content = postId == null ? null : contents.findById(postId).orElse(null);
        return content == null ? null : content.getCatalogAssetId();
    }
}
