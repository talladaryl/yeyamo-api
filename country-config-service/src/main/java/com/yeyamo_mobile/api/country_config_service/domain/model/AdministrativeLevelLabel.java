package com.yeyamo_mobile.api.country_config_service.domain.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Configuration of administrative level labels per country.
 * 
 * Allows each country to define its own terminology:
 * - Cameroon: Level 1 = Région, Level 2 = Département, Level 3 = Arrondissement
 * - Nigeria: Level 1 = State, Level 2 = Local Government Area
 * - Kenya: Level 1 = County, Level 2 = Sub-County, Level 3 = Ward
 * - South Africa: Level 1 = Province, Level 2 = District, Level 3 = Municipality
 */
@Entity
@Table(name = "administrative_level_labels", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"countryCode", "level"}),
       indexes = {
           @Index(name = "idx_admin_label_country", columnList = "countryCode")
       })
public class AdministrativeLevelLabel {

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
     * Administrative level (1, 2, 3...)
     */
    @NotNull
    @Min(1)
    @Max(10)
    @Column(nullable = false)
    private Integer level;

    /**
     * Label in the default language
     * Example: "Région" for Cameroon level 1
     */
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String label;

    /**
     * Plural form
     * Example: "Régions" for Cameroon level 1
     */
    @Size(max = 100)
    @Column(length = 100)
    private String labelPlural;

    /**
     * Localized labels
     * Example: {"fr": "Région", "en": "Region"}
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> localizedLabels = new HashMap<>();

    /**
     * Display order in hierarchies
     */
    @Column(nullable = false)
    private Integer displayOrder;

    // Constructors
    protected AdministrativeLevelLabel() {
    }

    public AdministrativeLevelLabel(
            String countryCode,
            Integer level,
            String label,
            String labelPlural,
            Integer displayOrder
    ) {
        this.countryCode = countryCode;
        this.level = level;
        this.label = label;
        this.labelPlural = labelPlural;
        this.displayOrder = displayOrder;
    }

    // Business methods
    public void addLocalizedLabel(String languageCode, String localizedLabel) {
        if (this.localizedLabels == null) {
            this.localizedLabels = new HashMap<>();
        }
        this.localizedLabels.put(languageCode, localizedLabel);
    }

    public String getLocalizedLabel(String languageCode) {
        if (localizedLabels != null && localizedLabels.containsKey(languageCode)) {
            return localizedLabels.get(languageCode);
        }
        return label;
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

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabelPlural() {
        return labelPlural;
    }

    public void setLabelPlural(String labelPlural) {
        this.labelPlural = labelPlural;
    }

    public Map<String, String> getLocalizedLabels() {
        return localizedLabels;
    }

    public void setLocalizedLabels(Map<String, String> localizedLabels) {
        this.localizedLabels = localizedLabels;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
