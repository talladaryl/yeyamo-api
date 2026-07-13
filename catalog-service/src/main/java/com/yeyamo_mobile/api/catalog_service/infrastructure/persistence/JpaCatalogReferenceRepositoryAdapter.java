package com.yeyamo_mobile.api.catalog_service.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.catalog_service.domain.model.CatalogReference;
import com.yeyamo_mobile.api.catalog_service.domain.model.ReferenceType;
import com.yeyamo_mobile.api.catalog_service.domain.port.CatalogReferenceRepository;

@Component
public class JpaCatalogReferenceRepositoryAdapter implements CatalogReferenceRepository {
    private final SpringDataCatalogReferenceRepository repository;
    public JpaCatalogReferenceRepositoryAdapter(SpringDataCatalogReferenceRepository repository) { this.repository = repository; }

    public CatalogReference save(CatalogReference value) { return domain(repository.save(entity(value))); }
    public Optional<CatalogReference> findById(UUID id) { return repository.findById(id).map(this::domain); }
    public Optional<CatalogReference> findByTypeAndCode(ReferenceType type, String code) { return repository.findByTypeAndCode(type, code).map(this::domain); }
    public boolean existsByTypeAndCode(ReferenceType type, String code) { return repository.existsByTypeAndCode(type, code); }
    public List<CatalogReference> find(ReferenceType type, String parentCode, boolean activeOnly, int limit) {
        var page = PageRequest.of(0, limit);
        List<CatalogReferenceEntity> found;
        if (parentCode != null) {
            found = activeOnly
                    ? repository.findByTypeAndParentCodeAndActiveOrderByName(type, parentCode, true, page)
                    : repository.findByTypeAndParentCodeOrderByName(type, parentCode, page);
        } else {
            found = activeOnly
                    ? repository.findByTypeAndActiveOrderByName(type, true, page)
                    : repository.findByTypeOrderByName(type, page);
        }
        return found.stream().map(this::domain).toList();
    }

    private CatalogReferenceEntity entity(CatalogReference v) {
        CatalogReferenceEntity e = new CatalogReferenceEntity();
        e.setId(v.getId()); e.setType(v.getType()); e.setCode(v.getCode()); e.setName(v.getName());
        e.setParentCode(v.getParentCode()); e.setCountryCode(v.getCountryCode()); e.setDescription(v.getDescription());
        e.setActive(v.isActive()); e.setCreatedAt(v.getCreatedAt()); e.setUpdatedAt(v.getUpdatedAt()); e.setVersion(v.getVersion());
        return e;
    }
    private CatalogReference domain(CatalogReferenceEntity e) {
        CatalogReference v = new CatalogReference();
        v.setId(e.getId()); v.setType(e.getType()); v.setCode(e.getCode()); v.setName(e.getName());
        v.setParentCode(e.getParentCode()); v.setCountryCode(e.getCountryCode()); v.setDescription(e.getDescription());
        v.setActive(e.isActive()); v.setCreatedAt(e.getCreatedAt()); v.setUpdatedAt(e.getUpdatedAt()); v.setVersion(e.getVersion());
        return v;
    }
}
