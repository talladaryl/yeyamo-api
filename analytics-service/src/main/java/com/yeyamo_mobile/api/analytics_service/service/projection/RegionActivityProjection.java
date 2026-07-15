package com.yeyamo_mobile.api.analytics_service.service.projection;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.analytics_service.event.AnalyticsDomainEvent;
import com.yeyamo_mobile.api.analytics_service.models.CatalogAssetDimension;
import com.yeyamo_mobile.api.analytics_service.models.ContentDimension;
import com.yeyamo_mobile.api.analytics_service.models.RegionActivity;
import com.yeyamo_mobile.api.analytics_service.repository.CatalogAssetDimensionRepository;
import com.yeyamo_mobile.api.analytics_service.repository.ContentDimensionRepository;
import com.yeyamo_mobile.api.analytics_service.repository.RegionActivityRepository;

@Component
public class RegionActivityProjection implements AnalyticsProjection {
    private final RegionActivityRepository regions;
    private final CatalogAssetDimensionRepository assets;
    private final ContentDimensionRepository contents;

    public RegionActivityProjection(RegionActivityRepository regions,
            CatalogAssetDimensionRepository assets, ContentDimensionRepository contents) {
        this.regions = regions;
        this.assets = assets;
        this.contents = contents;
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public boolean supports(AnalyticsDomainEvent event) {
        String type = event.eventType();
        return type.startsWith("user.profile.") || type.startsWith("catalog.asset.")
                || type.startsWith("content.post.") || type.equals("interaction.checkin.created");
    }

    @Override
    public void project(AnalyticsDomainEvent event) {
        String regionId = resolveRegion(event);
        if (regionId == null) {
            return;
        }
        String type = event.eventType();
        int users = type.equals("user.profile.created") ? 1
                : type.equals("user.profile.deleted") ? -1 : 0;
        int places = type.equals("catalog.asset.created") ? 1
                : type.equals("catalog.asset.deleted") ? -1 : 0;
        int posts = type.equals("content.post.published") ? 1
                : (type.equals("content.post.archived") || type.equals("content.post.deleted"))
                        && ProjectionSupport.text(event.payload(), "publishedAt") != null ? -1 : 0;
        int checkIns = type.equals("interaction.checkin.created") ? 1 : 0;
        String activeUser = ProjectionSupport.text(event.payload(), "userId", "authorId", "authUserId");
        if (activeUser == null) {
            activeUser = event.actorId();
        }
        LocalDate date = ProjectionSupport.date(event);
        UUID id = ProjectionSupport.id("region-activity", regionId, date);
        RegionActivity projection = regions.findById(id).orElseGet(RegionActivity::new);
        projection.setId(id);
        projection.setRegionId(regionId);
        projection.setActivityDate(date);
        if (projection.apply(event.eventId(), users, places, posts, checkIns, activeUser)) {
            regions.save(projection);
        }
    }

    private String resolveRegion(AnalyticsDomainEvent event) {
        String direct = ProjectionSupport.text(event.payload(), "regionId", "regionCode", "preferredRegionId");
        if (direct != null) {
            return direct;
        }
        UUID assetId = ProjectionSupport.uuid(event.payload(), "catalogAssetId", "placeId", "activityId", "assetId");
        if (assetId == null) {
            UUID postId = ProjectionSupport.uuid(event.payload(), "postId");
            ContentDimension content = postId == null ? null : contents.findById(postId).orElse(null);
            assetId = content == null ? null : content.getCatalogAssetId();
        }
        CatalogAssetDimension asset = assetId == null ? null : assets.findById(assetId).orElse(null);
        return asset == null ? null : asset.getRegionId();
    }
}
