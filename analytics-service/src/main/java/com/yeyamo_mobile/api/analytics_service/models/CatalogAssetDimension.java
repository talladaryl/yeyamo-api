package com.yeyamo_mobile.api.analytics_service.models;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import lombok.Getter;
import lombok.Setter;

@Document(indexName = "analytics_catalog_dimensions")
@Getter
@Setter
public class CatalogAssetDimension {
    @Id
    private UUID assetId;
    private String ownerId;
    private String regionId;
    private String assetType;
    private String status;
}
