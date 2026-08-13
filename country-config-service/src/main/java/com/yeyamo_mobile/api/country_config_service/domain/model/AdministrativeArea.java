package com.yeyamo_mobile.api.country_config_service.domain.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Generic administrative area model supporting diverse African administrative structures.
 * 
 * NOT tied to Cameroon-specific concepts (région, département, arrondissement).
 * Uses level-based hierarchy with country-specific labels.
 * 
 * Examples:
 * - Cameroon: Level 1=Région, Level 2=Département, Level 3=Arrondissement
 * - Nigeria: Level 1=State, Level 2=Local Government Area
 * - Kenya: Level 1=County, Level 2=Sub-County, Level 3=Ward
 * - South Africa: Level 1=Province, Level 2=District Municipality, Level 3=Local Municipality
 */
@Entity
@Table(name = "administrative_areas", indexes = {
    @Index(name = "idx_admin_area_country", columnList = "countryCode"),
    @Index(name = "idx_admin_area_parent", columnList = "parentId"),
    @Index(name = "idx_admin_area_level", columnList = "level"),
    @Index(name = "idx_admin_area_slug", columnList = "slug"),
    @Index(name = "idx_admin_area_country_level", columnList = "countryCode,level"),
    @Index(name = "idx_admin_area_country_parent", columnList = "countryCode,parentId")
})
@EntityListeners(AuditingEntityListener.class)
public class AdministrativeArea {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Country code (ISO 3166-1 alpha-2)
     */
    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[A-Z]{2}")
    @Column(nullable = false, length = 2)
    private String countryCode;

    /**
     * Parent administrative area (null for top-level)
     */
    @Column
    private UUID parentId;

    /**
     * Hierarchical level (1=top, 2, 3...)
     * Level 1: Region, State, Province, County...
     * Level 2: Department, LGA, District, Sub-County...
     * Level 3: Arrondissement, Commune, Ward...
     */
    @NotNull
    @Min(1)
    @Max(10)
    @Column(nullable = false)
    private Integer level;

    /**
     * Type code for this area (optional, for additional classification)
     * Examples: "urban_commune", "rural_district", "autonomous_city"
     */
    @Size(max = 50)
    @Column(length = 50)
    private String typeCode;

    /**
     * Official name of the area
     */
    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Localized names in different languages
     * Example: {"fr": "Littoral", "en": "Littoral", "local": "Ndé"}
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> localizedNames = new HashMap<>();

    /**
     * Official administrative code (if exists)
     * Example: "01" for Adamawa region in Cameroon
     */
    @Size(max = 20)
    @Column(length = 20)
    private String officialCode;

    /**
     * URL-friendly slug
     */
    @NotBlank
    @Size(max = 250)
    @Column(nullable = false, length = 250)
    private String slug;

    /**
     * Latitude (center point)
     */
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    /**
     * Longitude (center point)
     */
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    /**
     * Is this area active?
     */
    @NotNull
    @Column(nullable = false)
    private Boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    // Constructors
    protected AdministrativeArea() {
    }

    public AdministrativeArea(
            String countryCode,
            UUID parentId,
            Integer level,
            String name,
            String slug
    ) {
        this.countryCode = countryCode;
        this.parentId = parentId;
        this.level = level;
        this.name = name;
        this.slug = slug;
    }

    // Business methods
    public boolean isTopLevel() {
        return level == 1;
    }

    public boolean hasParent() {
        return parentId != null;
    }

    public void addLocalizedName(String languageCode, String localizedName) {
        if (this.localizedNames == null) {
            this.localizedNames = new HashMap<>();
        }
        this.localizedNames.put(languageCode, localizedName);
    }

    public String getLocalizedName(String languageCode) {
        if (localizedNames != null && localizedNames.containsKey(languageCode)) {
            return localizedNames.get(languageCode);
        }
        return name;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getLocalizedNames() {
        return localizedNames;
    }

    public void setLocalizedNames(Map<String, String> localizedNames) {
        this.localizedNames = localizedNames;
    }

    public String getOfficialCode() {
        return officialCode;
    }

    public void setOfficialCode(String officialCode) {
        this.officialCode = officialCode;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
