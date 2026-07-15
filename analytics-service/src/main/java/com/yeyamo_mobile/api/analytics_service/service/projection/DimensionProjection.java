package com.yeyamo_mobile.api.analytics_service.service.projection;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.analytics_service.event.AnalyticsDomainEvent;
import com.yeyamo_mobile.api.analytics_service.models.CatalogAssetDimension;
import com.yeyamo_mobile.api.analytics_service.models.ContentDimension;
import com.yeyamo_mobile.api.analytics_service.models.PartnerDimension;
import com.yeyamo_mobile.api.analytics_service.repository.CatalogAssetDimensionRepository;
import com.yeyamo_mobile.api.analytics_service.repository.ContentDimensionRepository;
import com.yeyamo_mobile.api.analytics_service.repository.PartnerDimensionRepository;

@Component
public class DimensionProjection implements AnalyticsProjection {
    private final CatalogAssetDimensionRepository catalogAssets;
    private final ContentDimensionRepository contents;
    private final PartnerDimensionRepository partners;

    public DimensionProjection(CatalogAssetDimensionRepository catalogAssets,
            ContentDimensionRepository contents, PartnerDimensionRepository partners) {
        this.catalogAssets = catalogAssets;
        this.contents = contents;
        this.partners = partners;
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public boolean supports(AnalyticsDomainEvent event) {
        return event.eventType().startsWith("catalog.asset.")
                || event.eventType().startsWith("content.post.")
                || event.eventType().startsWith("partner.");
    }

    @Override
    public void project(AnalyticsDomainEvent event) {
        if (event.eventType().startsWith("catalog.asset.")) {
            projectCatalog(event);
        } else if (event.eventType().startsWith("content.post.")) {
            projectContent(event);
        } else {
            projectPartner(event);
        }
    }

    private void projectCatalog(AnalyticsDomainEvent event) {
        UUID id = ProjectionSupport.uuid(event.payload(), "assetId", "placeId");
        if (id == null) {
            return;
        }
        CatalogAssetDimension dimension = catalogAssets.findById(id).orElseGet(CatalogAssetDimension::new);
        dimension.setAssetId(id);
        dimension.setOwnerId(ProjectionSupport.text(event.payload(), "ownerId", "ownerUserId"));
        dimension.setRegionId(ProjectionSupport.text(event.payload(), "regionId", "regionCode"));
        dimension.setAssetType(ProjectionSupport.text(event.payload(), "type", "assetType"));
        dimension.setStatus(ProjectionSupport.text(event.payload(), "status"));
        catalogAssets.save(dimension);
    }

    private void projectContent(AnalyticsDomainEvent event) {
        UUID id = ProjectionSupport.uuid(event.payload(), "postId", "id");
        if (id == null) {
            return;
        }
        ContentDimension dimension = contents.findById(id).orElseGet(ContentDimension::new);
        dimension.setPostId(id);
        dimension.setAuthorId(ProjectionSupport.text(event.payload(), "authorId"));
        dimension.setCatalogAssetId(ProjectionSupport.uuid(event.payload(), "catalogAssetId", "placeId"));
        dimension.setStatus(ProjectionSupport.text(event.payload(), "status"));
        contents.save(dimension);
    }

    private void projectPartner(AnalyticsDomainEvent event) {
        UUID id = ProjectionSupport.uuid(event.payload(), "partnerId");
        if (id == null) {
            return;
        }
        PartnerDimension dimension = partners.findById(id).orElseGet(PartnerDimension::new);
        dimension.setPartnerId(id);
        String owner = ProjectionSupport.text(event.payload(), "ownerUserId", "requesterId");
        if (owner != null) {
            dimension.setOwnerUserId(owner);
        }
        dimension.setStatus(ProjectionSupport.text(event.payload(), "status"));
        partners.save(dimension);
    }
}
