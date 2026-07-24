package com.yeyamo_mobile.api.catalog_service.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataCollectionRepository extends JpaRepository<CollectionEntity, UUID> {
    
    // Mes collections (paginé, trié par updated_at DESC)
    Page<CollectionEntity> findByUserIdOrderByUpdatedAtDesc(String userId, Pageable pageable);
    
    // Collections publiques (paginé, trié par updated_at DESC)
    Page<CollectionEntity> findByIsPublicTrueOrderByUpdatedAtDesc(Pageable pageable);
    
    // Vérifier l'existence et la propriété
    boolean existsByIdAndUserId(UUID id, String userId);
    
    // Summaries : collections d'un utilisateur avec compteur de lieux
    @Query("""
        SELECT c.id, c.title, c.coverAssetId, COUNT(cp.assetId) as placeCount
        FROM CollectionEntity c
        LEFT JOIN CollectionPlaceEntity cp ON c.id = cp.collectionId
        WHERE c.userId = :userId
        GROUP BY c.id, c.title, c.coverAssetId, c.updatedAt
        ORDER BY c.updatedAt DESC
        """)
    List<Object[]> findSummariesByUserId(@Param("userId") String userId);
}
