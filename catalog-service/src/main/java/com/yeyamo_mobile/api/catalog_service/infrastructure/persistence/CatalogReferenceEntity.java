package com.yeyamo_mobile.api.catalog_service.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.yeyamo_mobile.api.catalog_service.domain.model.ReferenceType;

import jakarta.persistence.*;

@Entity
@Table(name = "catalog_references", uniqueConstraints =
        @UniqueConstraint(name = "uk_catalog_reference_type_code", columnNames = { "type", "code" }))
public class CatalogReferenceEntity {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private ReferenceType type;
    @Column(nullable = false, length = 80) private String code;
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "parent_code", length = 80) private String parentCode;
    @Column(name = "country_code", length = 2) private String countryCode;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public ReferenceType getType(){return type;} public void setType(ReferenceType v){type=v;}
    public String getCode(){return code;} public void setCode(String v){code=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getParentCode(){return parentCode;} public void setParentCode(String v){parentCode=v;}
    public String getCountryCode(){return countryCode;} public void setCountryCode(String v){countryCode=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
    public long getVersion(){return version;} public void setVersion(long v){version=v;}
}
