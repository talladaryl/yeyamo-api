package com.yeyamo_mobile.api.place_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yeyamo_mobile.api.place_service.enums.PlaceStatus;
import com.yeyamo_mobile.api.place_service.models.Place;

public interface PlaceRepository extends JpaRepository<Place, UUID> {

    Optional<Place> findBySlug(String slug);

    @Query("""
            SELECT p FROM Place p
            JOIN FETCH p.region
            JOIN FETCH p.city
            LEFT JOIN FETCH p.district
            LEFT JOIN FETCH p.category
            WHERE p.id = :id
            """)
    Optional<Place> findDetailedById(@Param("id") UUID id);

    @Query("""
            SELECT p FROM Place p
            JOIN FETCH p.region r
            JOIN FETCH p.city
            LEFT JOIN FETCH p.district
            LEFT JOIN FETCH p.category
            WHERE r.slug = :slug AND p.status = :status
            ORDER BY p.name ASC
            """)
    List<Place> findByRegionSlug(@Param("slug") String slug, @Param("status") PlaceStatus status);

    @Query("""
            SELECT p FROM Place p
            JOIN FETCH p.region
            JOIN FETCH p.city
            LEFT JOIN FETCH p.district
            LEFT JOIN FETCH p.category
            WHERE p.city.id = :cityId AND p.status = :status
            ORDER BY p.name ASC
            """)
    List<Place> findByCityId(@Param("cityId") Long cityId, @Param("status") PlaceStatus status);

    @Query(value = """
            SELECT p.* FROM places p
            WHERE ST_DWithin(
                p.location::geography,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                :radiusMeters
            )
            AND p.status = 'PUBLISHED'
            AND (:categoryId IS NULL OR p.category_id = :categoryId)
            ORDER BY ST_Distance(
                p.location::geography,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
            )
            LIMIT :limit
            """, nativeQuery = true)
    List<Place> findNearby(
            @Param("lat") double latitude,
            @Param("lng") double longitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("categoryId") Long categoryId,
            @Param("limit") int limit
    );

    boolean existsBySlug(String slug);
}
