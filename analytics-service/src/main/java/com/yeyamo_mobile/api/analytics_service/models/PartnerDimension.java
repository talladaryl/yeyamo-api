package com.yeyamo_mobile.api.analytics_service.models;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import lombok.Getter;
import lombok.Setter;

@Document(indexName = "analytics_partner_dimensions")
@Getter
@Setter
public class PartnerDimension {
    @Id
    private UUID partnerId;
    private String ownerUserId;
    private String status;
}
