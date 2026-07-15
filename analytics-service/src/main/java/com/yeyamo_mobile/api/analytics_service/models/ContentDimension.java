package com.yeyamo_mobile.api.analytics_service.models;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import lombok.Getter;
import lombok.Setter;

@Document(indexName = "analytics_content_dimensions")
@Getter
@Setter
public class ContentDimension {
    @Id
    private UUID postId;
    private String authorId;
    private UUID catalogAssetId;
    private String status;
}
