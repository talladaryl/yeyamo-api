package com.yeyamo_mobile.api.catalog_service.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.catalog_service.domain.model.ReferenceType;

public interface SpringDataCatalogReferenceRepository extends JpaRepository<CatalogReferenceEntity, UUID> {
    Optional<CatalogReferenceEntity> findByTypeAndCode(ReferenceType type, String code);
    boolean existsByTypeAndCode(ReferenceType type, String code);
    List<CatalogReferenceEntity> findByTypeAndActiveOrderByName(ReferenceType type, boolean active, Pageable page);
    List<CatalogReferenceEntity> findByTypeOrderByName(ReferenceType type, Pageable page);
    List<CatalogReferenceEntity> findByTypeAndParentCodeAndActiveOrderByName(ReferenceType type, String parentCode, boolean active, Pageable page);
    List<CatalogReferenceEntity> findByTypeAndParentCodeOrderByName(ReferenceType type, String parentCode, Pageable page);
}
