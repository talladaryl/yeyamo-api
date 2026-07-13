package com.yeyamo_mobile.api.catalog_service.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yeyamo_mobile.api.catalog_service.domain.model.AssetStatus;
import com.yeyamo_mobile.api.catalog_service.domain.model.AssetType;

public interface SpringDataCatalogAssetRepository extends JpaRepository<CatalogAssetEntity, UUID> {
    Optional<CatalogAssetEntity> findBySlug(String slug);
    Optional<CatalogAssetEntity> findBySourceAndExternalId(String source, String externalId);
    boolean existsBySlugAndIdNot(String slug, UUID id);

    @Query("""
            select a from CatalogAssetEntity a
            where (:status is null or a.status = :status)
              and (:type is null or a.type = :type)
              and (:regionCode is null or a.regionCode = :regionCode)
              and (:categoryCode is null or a.categoryCode = :categoryCode)
              and (:query is null or lower(a.name) like lower(concat('%', :query, '%'))
                   or lower(a.description) like lower(concat('%', :query, '%')))
            order by a.updatedAt desc
            limit :limit
            """)
    List<CatalogAssetEntity> search(@Param("status") AssetStatus status, @Param("type") AssetType type,
            @Param("regionCode") String regionCode, @Param("categoryCode") String categoryCode,
            @Param("query") String query, @Param("limit") int limit);

    @Query(value = """
            select * from catalog_assets a
            where a.status = 'PUBLISHED'
              and (:type is null or a.type = cast(:type as varchar))
              and (:categoryCode is null or a.category_code = :categoryCode)
              and ST_DWithin(a.location::geography,
                  ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters)
            order by ST_Distance(a.location::geography,
                  ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography)
            limit :limit
            """, nativeQuery = true)
    List<CatalogAssetEntity> findNearby(@Param("latitude") double latitude,
            @Param("longitude") double longitude, @Param("radiusMeters") double radiusMeters,
            @Param("type") String type, @Param("categoryCode") String categoryCode,
            @Param("limit") int limit);
}
