package com.yeyamo_mobile.api.catalog_service.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.catalog_service.domain.model.AssetStatus;
import com.yeyamo_mobile.api.catalog_service.domain.model.AssetType;
import com.yeyamo_mobile.api.catalog_service.domain.model.CatalogAsset;
import com.yeyamo_mobile.api.catalog_service.domain.model.GeoPoint;
import com.yeyamo_mobile.api.catalog_service.domain.port.CatalogAssetRepository;

@Component
public class JpaCatalogAssetRepositoryAdapter implements CatalogAssetRepository {
    private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), 4326);
    private final SpringDataCatalogAssetRepository repository;

    public JpaCatalogAssetRepositoryAdapter(SpringDataCatalogAssetRepository repository) {
        this.repository = repository;
    }

    @Override public CatalogAsset save(CatalogAsset asset) { return toDomain(repository.save(toEntity(asset))); }
    @Override public Optional<CatalogAsset> findById(UUID id) { return repository.findById(id).map(this::toDomain); }
    @Override public boolean existsById(UUID id) { return repository.existsById(id); }
    @Override public List<CatalogAsset> findAllById(List<UUID> ids) { 
        return repository.findAllById(ids).stream().map(this::toDomain).toList(); 
    }
    @Override public Optional<CatalogAsset> findBySlug(String slug) { return repository.findBySlug(slug).map(this::toDomain); }
    @Override public Optional<CatalogAsset> findBySourceAndExternalId(String source, String externalId) {
        return repository.findBySourceAndExternalId(source, externalId).map(this::toDomain);
    }
    @Override public boolean existsBySlugAndIdNot(String slug, UUID id) { return repository.existsBySlugAndIdNot(slug, id); }
    @Override public List<CatalogAsset> search(AssetStatus status, AssetType type, String regionCode,
            String categoryCode, String query, int limit) {
        return repository.search(status, type, blankToNull(regionCode), blankToNull(categoryCode),
                blankToNull(query), limit).stream().map(this::toDomain).toList();
    }
    @Override public List<CatalogAsset> findNearby(double latitude, double longitude, double radiusMeters,
            AssetType type, String categoryCode, int limit) {
        return repository.findNearby(latitude, longitude, radiusMeters,
                type == null ? null : type.name(), blankToNull(categoryCode), limit).stream()
                .map(this::toDomain).toList();
    }

    private CatalogAssetEntity toEntity(CatalogAsset a) {
        CatalogAssetEntity e = new CatalogAssetEntity();
        e.setId(a.getId()); e.setType(a.getType()); e.setOwnerId(a.getOwnerId());
        e.setSource(a.getSource()); e.setExternalId(a.getExternalId()); e.setName(a.getName());
        e.setSlug(a.getSlug()); e.setDescription(a.getDescription()); e.setCategoryCode(a.getCategoryCode()); e.setCountryCode(a.getCountryCode());
        e.setRegionCode(a.getRegionCode()); e.setCity(a.getCity()); e.setDistrict(a.getDistrict());
        e.setAddress(a.getAddress()); e.setLatitude(a.getLocation().latitude());
        e.setLongitude(a.getLocation().longitude());
        e.setLocation(GEOMETRY.createPoint(new Coordinate(a.getLocation().longitude(), a.getLocation().latitude())));
        e.setMediaIds(a.getMediaIds()); e.setDurationMinutes(a.getDurationMinutes()); e.setDifficultyLevel(a.getDifficultyLevel());
        e.setPrice(a.getPrice()); e.setCurrency(a.getCurrency()); e.setCapacityMin(a.getCapacityMin()); e.setCapacityMax(a.getCapacityMax());
        e.setIncludedItems(a.getIncludedItems()); e.setExcludedItems(a.getExcludedItems()); e.setPlaceId(a.getPlaceId());
        e.setStatus(a.getStatus()); e.setCreatedAt(a.getCreatedAt()); e.setUpdatedAt(a.getUpdatedAt());
        e.setVersion(a.getVersion());
        return e;
    }
    private CatalogAsset toDomain(CatalogAssetEntity e) {
        CatalogAsset a = new CatalogAsset();
        a.setId(e.getId()); a.setType(e.getType()); a.setOwnerId(e.getOwnerId());
        a.setSource(e.getSource()); a.setExternalId(e.getExternalId()); a.setName(e.getName());
        a.setSlug(e.getSlug()); a.setDescription(e.getDescription()); a.setCategoryCode(e.getCategoryCode()); a.setCountryCode(e.getCountryCode());
        a.setRegionCode(e.getRegionCode()); a.setCity(e.getCity()); a.setDistrict(e.getDistrict());
        a.setAddress(e.getAddress()); a.setLocation(new GeoPoint(e.getLatitude(), e.getLongitude()));
        a.setMediaIds(e.getMediaIds()); a.setDurationMinutes(e.getDurationMinutes()); a.setDifficultyLevel(e.getDifficultyLevel());
        a.setPrice(e.getPrice()); a.setCurrency(e.getCurrency()); a.setCapacityMin(e.getCapacityMin()); a.setCapacityMax(e.getCapacityMax());
        a.setIncludedItems(e.getIncludedItems()); a.setExcludedItems(e.getExcludedItems()); a.setPlaceId(e.getPlaceId());
        a.setStatus(e.getStatus()); a.setCreatedAt(e.getCreatedAt()); a.setUpdatedAt(e.getUpdatedAt());
        a.setVersion(e.getVersion());
        return a;
    }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
