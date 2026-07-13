package com.yeyamo_mobile.api.catalog_service.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.yeyamo_mobile.api.catalog_service.domain.model.CatalogReference;
import com.yeyamo_mobile.api.catalog_service.domain.model.ReferenceType;

public interface CatalogReferenceRepository {
    CatalogReference save(CatalogReference reference);
    Optional<CatalogReference> findById(UUID id);
    Optional<CatalogReference> findByTypeAndCode(ReferenceType type, String code);
    boolean existsByTypeAndCode(ReferenceType type, String code);
    List<CatalogReference> find(ReferenceType type, String parentCode, boolean activeOnly, int limit);
}
