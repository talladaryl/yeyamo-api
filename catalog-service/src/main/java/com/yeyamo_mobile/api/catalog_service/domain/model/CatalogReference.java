package com.yeyamo_mobile.api.catalog_service.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public class CatalogReference {
    private UUID id;
    private ReferenceType type;
    private String code;
    private String name;
    private String parentCode;
    private String countryCode;
    private String description;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    public static CatalogReference create(ReferenceType type, String code, String name,
            String parentCode, String countryCode, String description) {
        CatalogReference reference = new CatalogReference();
        reference.id = UUID.randomUUID();
        reference.type = require(type, "type");
        reference.code = normalizeCode(code);
        reference.name = requireText(name, "name");
        reference.parentCode = normalizeNullableCode(parentCode);
        reference.countryCode = normalizeCountry(countryCode);
        reference.description = trimToNull(description);
        reference.validateHierarchy();
        reference.active = true;
        reference.createdAt = Instant.now();
        reference.updatedAt = reference.createdAt;
        return reference;
    }

    public void update(String name, String parentCode, String countryCode, String description) {
        this.name = requireText(name, "name");
        this.parentCode = normalizeNullableCode(parentCode);
        this.countryCode = normalizeCountry(countryCode);
        this.description = trimToNull(description);
        validateHierarchy();
        updatedAt = Instant.now();
    }

    public void deactivate() { active = false; updatedAt = Instant.now(); }
    public void activate() { active = true; updatedAt = Instant.now(); }

    private void validateHierarchy() {
        if (type == ReferenceType.CITY && parentCode == null) {
            throw new IllegalArgumentException("A city requires its regionCode as parentCode");
        }
        if (type == ReferenceType.REGION && countryCode == null) {
            throw new IllegalArgumentException("A region requires countryCode");
        }
    }

    private static String normalizeCode(String value) {
        String code = requireText(value, "code").toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9][A-Z0-9_-]{0,79}")) throw new IllegalArgumentException("Invalid reference code");
        return code;
    }
    private static String normalizeNullableCode(String value) { return trimToNull(value) == null ? null : normalizeCode(value); }
    private static String normalizeCountry(String value) {
        String country = trimToNull(value);
        if (country == null) return null;
        country = country.toUpperCase(Locale.ROOT);
        if (!country.matches("[A-Z]{2}")) throw new IllegalArgumentException("countryCode must use ISO alpha-2 format");
        return country;
    }
    private static String requireText(String value, String field) {
        String normalized = trimToNull(value);
        if (normalized == null) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }
    private static <T> T require(T value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " is required");
        return value;
    }
    private static String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public UUID getId() { return id; } public void setId(UUID v) { id = v; }
    public ReferenceType getType() { return type; } public void setType(ReferenceType v) { type = v; }
    public String getCode() { return code; } public void setCode(String v) { code = v; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getParentCode() { return parentCode; } public void setParentCode(String v) { parentCode = v; }
    public String getCountryCode() { return countryCode; } public void setCountryCode(String v) { countryCode = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public boolean isActive() { return active; } public void setActive(boolean v) { active = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt = v; }
    public long getVersion() { return version; } public void setVersion(long v) { version = v; }
}
