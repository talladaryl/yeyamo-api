package com.yeyamo_mobile.api.catalog_service.application;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.catalog_service.domain.model.CatalogReference;
import com.yeyamo_mobile.api.catalog_service.domain.model.ReferenceType;
import com.yeyamo_mobile.api.catalog_service.domain.port.CatalogReferenceRepository;
import com.yeyamo_mobile.api.catalog_service.application.port.CatalogReferenceOutboxPort;

@Service
@Transactional
public class CatalogReferenceService {
    private final CatalogReferenceRepository repository;
    private final CatalogReferenceOutboxPort outbox;

    public CatalogReferenceService(CatalogReferenceRepository repository, CatalogReferenceOutboxPort outbox) { this.repository = repository; this.outbox = outbox; }

    public CatalogReference create(ReferenceType type, String code, String name, String parentCode,
            String countryCode, String description, String correlationId, String actorId) {
        String normalized = normalize(code);
        if (repository.existsByTypeAndCode(type, normalized)) {
            throw new CatalogException("CATALOG_REFERENCE_EXISTS", "Reference code already exists for " + type);
        }
        validateParent(type, parentCode);
        CatalogReference saved=repository.save(CatalogReference.create(type, code, name, parentCode, countryCode, description));
        outbox.append("catalog.reference.created",saved,correlationId,actorId);return saved;
    }

    public CatalogReference update(UUID id, String name, String parentCode, String countryCode, String description, String correlationId, String actorId) {
        CatalogReference reference = required(id);
        validateParent(reference.getType(), parentCode);
        reference.update(name, parentCode, countryCode, description);
        CatalogReference saved=repository.save(reference);outbox.append("catalog.reference.updated",saved,correlationId,actorId);return saved;
    }

    public CatalogReference setActive(UUID id, boolean active, String correlationId, String actorId) {
        CatalogReference reference = required(id);
        if (active) reference.activate(); else reference.deactivate();
        CatalogReference saved=repository.save(reference);outbox.append(active?"catalog.reference.activated":"catalog.reference.deactivated",saved,correlationId,actorId);return saved;
    }

    @Transactional(readOnly = true)
    public CatalogReference get(UUID id) { return required(id); }

    @Transactional(readOnly = true)
    public CatalogReference getPublic(UUID id) { CatalogReference value=required(id);if(!value.isActive())throw new CatalogException("CATALOG_REFERENCE_NOT_FOUND","Catalog reference not found");return value; }

    @Transactional(readOnly = true)
    public List<CatalogReference> list(ReferenceType type, String parentCode, boolean activeOnly, int limit) {
        return repository.find(type, normalizeNullable(parentCode), activeOnly, Math.max(1, Math.min(limit, 200)));
    }

    private void validateParent(ReferenceType type, String parentCode) {
        if (type == ReferenceType.CITY) {
            String code = normalizeNullable(parentCode);
            if (code == null || repository.findByTypeAndCode(ReferenceType.REGION, code).filter(CatalogReference::isActive).isEmpty()) {
                throw new CatalogException("CATALOG_REGION_NOT_FOUND", "Active parent region not found");
            }
        }
        if (type == ReferenceType.CATEGORY && parentCode != null
                && repository.findByTypeAndCode(ReferenceType.CATEGORY, normalize(parentCode)).isEmpty()) {
            throw new CatalogException("CATALOG_PARENT_CATEGORY_NOT_FOUND", "Parent category not found");
        }
    }

    private CatalogReference required(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new CatalogException("CATALOG_REFERENCE_NOT_FOUND", "Catalog reference not found"));
    }
    private String normalize(String code) { return code == null ? null : code.trim().toUpperCase(Locale.ROOT); }
    private String normalizeNullable(String code) { return code == null || code.isBlank() ? null : normalize(code); }
}
