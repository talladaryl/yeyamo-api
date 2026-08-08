package com.yeyamo_mobile.api.catalog_service.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="artworks")
public class ArtworkEntity {
    public enum EditionType { UNIQUE, LIMITED_EDITION, SERIES, REPRODUCTION, CUSTOM_ORDER }
    public enum AvailabilityStatus { DISPLAY_ONLY, AVAILABLE, ON_ORDER, RESERVED, SOLD, UNAVAILABLE }
    public enum AuthenticityStatus { DECLARED, PROCESS_VERIFIED, EXPERT_VERIFIED, INSTITUTION_VERIFIED, DISPUTED }
    @Id @Column(name="asset_id") public UUID assetId;
    @Column(name="artisan_partner_id",nullable=false) public UUID artisanPartnerId;
    @Column(nullable=false) public String title;
    @Column(nullable=false,unique=true) public String slug;
    @Column(name="short_description") public String shortDescription;
    @Column(columnDefinition="TEXT") public String story;
    @Column(name="country_code",nullable=false,length=2) public String countryCode;
    @Column(name="admin_level_1_id") public String adminLevel1Id;
    @Column(name="city_id") public String cityId;
    @Column(name="locality_id") public String localityId;
    @Column(name="culture_content_id") public UUID cultureContentId;
    @Column(name="cultural_community") public String culturalCommunity;
    @Column(name="year_created") public Integer yearCreated;
    @Column(name="production_time") public String productionTime;
    public BigDecimal width; public BigDecimal height; public BigDecimal depth; public BigDecimal weight;
    @Enumerated(EnumType.STRING) @Column(name="edition_type",nullable=false) public EditionType editionType;
    @Column(name="edition_size") public Integer editionSize;
    @Enumerated(EnumType.STRING) @Column(name="availability_status",nullable=false) public AvailabilityStatus availabilityStatus;
    @Enumerated(EnumType.STRING) @Column(name="authenticity_status",nullable=false) public AuthenticityStatus authenticityStatus;
    @Column(name="deleted_at") public Instant deletedAt;
    @Column(name="created_at",nullable=false,updatable=false) public Instant createdAt;
    @Column(name="updated_at",nullable=false) public Instant updatedAt;
    @Version public long version;
    @PrePersist void create(){createdAt=updatedAt=Instant.now();}
    @PreUpdate void update(){updatedAt=Instant.now();}
}
