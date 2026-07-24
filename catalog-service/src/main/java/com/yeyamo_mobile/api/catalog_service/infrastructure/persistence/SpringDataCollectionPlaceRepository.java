package com.yeyamo_mobile.api.catalog_service.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataCollectionPlaceRepository extends JpaRepository<CollectionPlaceEntity, CollectionPlaceEntity.CollectionPlaceId> {
    
    // Récupérer les lieux d'une collection (paginé, trié par added_at DESC)
    Page<CollectionPlaceEntity> findByCollectionIdOrderByAddedAtDesc(UUID collectionId, Pageable pageable);
    
    // Compter les lieux dans une collection
    long countByCollectionId(UUID collectionId);
    
    // Vérifier si un lieu est dans une collection (pour idempotence)
    boolean existsByCollectionIdAndAssetId(UUID collectionId, UUID assetId);

    Optional<CollectionPlaceEntity> findByCollectionIdAndAssetId(UUID collectionId, UUID assetId);
    
    // IDs des assets dans une collection (pour récupération en bulk)
    @Query("SELECT cp.assetId FROM CollectionPlaceEntity cp WHERE cp.collectionId = :collectionId ORDER BY cp.addedAt DESC")
    List<UUID> findAssetIdsByCollectionId(UUID collectionId);
}
