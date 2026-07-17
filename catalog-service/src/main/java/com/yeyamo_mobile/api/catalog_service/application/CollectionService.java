package com.yeyamo_mobile.api.catalog_service.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.catalog_service.application.port.OutboxPort;
import com.yeyamo_mobile.api.catalog_service.domain.model.CatalogAsset;
import com.yeyamo_mobile.api.catalog_service.domain.port.CatalogAssetRepository;
import com.yeyamo_mobile.api.catalog_service.infrastructure.persistence.CollectionEntity;
import com.yeyamo_mobile.api.catalog_service.infrastructure.persistence.CollectionPlaceEntity;
import com.yeyamo_mobile.api.catalog_service.infrastructure.persistence.SpringDataCollectionPlaceRepository;
import com.yeyamo_mobile.api.catalog_service.infrastructure.persistence.SpringDataCollectionRepository;

@Service
public class CollectionService {
    
    private final SpringDataCollectionRepository collectionRepository;
    private final SpringDataCollectionPlaceRepository collectionPlaceRepository;
    private final CatalogAssetRepository assetRepository;
    private final OutboxPort outbox;

    public CollectionService(
            SpringDataCollectionRepository collectionRepository,
            SpringDataCollectionPlaceRepository collectionPlaceRepository,
            CatalogAssetRepository assetRepository,
            OutboxPort outbox) {
        this.collectionRepository = collectionRepository;
        this.collectionPlaceRepository = collectionPlaceRepository;
        this.assetRepository = assetRepository;
        this.outbox = outbox;
    }

    // ─── CRUD COLLECTIONS ───────────────────────────────────────────────────────

    @Transactional
    public CollectionEntity create(UUID userId, String title, String description, boolean isPublic, 
            UUID coverAssetId, String correlationId, String actorId) {
        
        // Vérifier que le cover asset existe si fourni
        if (coverAssetId != null && !assetRepository.existsById(coverAssetId)) {
            throw new CatalogException("ASSET_NOT_FOUND", "Asset de couverture introuvable", HttpStatus.BAD_REQUEST);
        }

        CollectionEntity collection = new CollectionEntity();
        collection.setId(UUID.randomUUID());
        collection.setUserId(userId);
        collection.setTitle(title);
        collection.setDescription(description);
        collection.setPublic(isPublic);
        collection.setCoverAssetId(coverAssetId);
        collection.setCreatedAt(Instant.now());
        collection.setUpdatedAt(Instant.now());

        CollectionEntity saved = collectionRepository.save(collection);
        
        outbox.append("catalog.collection.created", saved.getId().toString(), actorId, correlationId,
                java.util.Map.of("collectionId", saved.getId().toString(), "userId", userId.toString(), "title", title));
        
        return saved;
    }

    @Transactional
    public CollectionEntity update(UUID collectionId, UUID requesterId, String title, String description, 
            Boolean isPublic, UUID coverAssetId, String correlationId, String actorId) {
        
        CollectionEntity collection = getRequired(collectionId);
        
        // IDOR Protection: vérifier la propriété
        if (!collection.getUserId().equals(requesterId)) {
            throw new CatalogException("FORBIDDEN", "Vous n'êtes pas propriétaire de cette collection", HttpStatus.FORBIDDEN);
        }

        // Vérifier que le cover asset existe si fourni
        if (coverAssetId != null && !assetRepository.existsById(coverAssetId)) {
            throw new CatalogException("ASSET_NOT_FOUND", "Asset de couverture introuvable", HttpStatus.BAD_REQUEST);
        }

        if (title != null) collection.setTitle(title);
        if (description != null) collection.setDescription(description);
        if (isPublic != null) collection.setPublic(isPublic);
        if (coverAssetId != null) collection.setCoverAssetId(coverAssetId);
        collection.setUpdatedAt(Instant.now());

        CollectionEntity saved = collectionRepository.save(collection);
        
        outbox.append("catalog.collection.updated", saved.getId().toString(), actorId, correlationId,
                java.util.Map.of("collectionId", saved.getId().toString()));
        
        return saved;
    }

    @Transactional
    public void delete(UUID collectionId, UUID requesterId, String correlationId, String actorId) {
        CollectionEntity collection = getRequired(collectionId);
        
        // IDOR Protection: vérifier la propriété
        if (!collection.getUserId().equals(requesterId)) {
            throw new CatalogException("FORBIDDEN", "Vous n'êtes pas propriétaire de cette collection", HttpStatus.FORBIDDEN);
        }

        collectionRepository.deleteById(collectionId);
        
        outbox.append("catalog.collection.deleted", collectionId.toString(), actorId, correlationId,
                java.util.Map.of("collectionId", collectionId.toString()));
    }

    // ─── LECTURE COLLECTIONS ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CollectionEntity> getMyCollections(UUID userId, Pageable pageable) {
        return collectionRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CollectionEntity> getPublicCollections(Pageable pageable) {
        return collectionRepository.findByIsPublicTrueOrderByUpdatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public CollectionWithAssets getCollection(UUID collectionId, UUID requesterId) {
        CollectionEntity collection = getRequired(collectionId);
        
        // Vérification de visibilité : public OU propriétaire
        if (!collection.isPublic() && !collection.getUserId().equals(requesterId)) {
            // Retourner 404 (pas 403) pour ne pas révéler l'existence d'une collection privée
            throw new CatalogException("COLLECTION_NOT_FOUND", "Collection introuvable", HttpStatus.NOT_FOUND);
        }

        // Charger les assets de la collection
        List<UUID> assetIds = collectionPlaceRepository.findAssetIdsByCollectionId(collectionId);
        List<CatalogAsset> assets = assetRepository.findAllById(assetIds);
        
        return new CollectionWithAssets(collection, assets);
    }

    @Transactional(readOnly = true)
    public List<CollectionSummary> getSummaries(UUID userId) {
        List<Object[]> results = collectionRepository.findSummariesByUserId(userId);
        
        return results.stream()
                .map(row -> new CollectionSummary(
                        (UUID) row[0],           // id
                        (String) row[1],         // title
                        (UUID) row[2],           // coverAssetId
                        ((Number) row[3]).longValue() // placeCount
                ))
                .collect(Collectors.toList());
    }

    // ─── GESTION LIEUX DANS COLLECTION ─────────────────────────────────────────

    @Transactional
    public void addPlace(UUID collectionId, UUID assetId, UUID requesterId, String correlationId, String actorId) {
        CollectionEntity collection = getRequired(collectionId);
        
        // IDOR Protection: vérifier la propriété
        if (!collection.getUserId().equals(requesterId)) {
            throw new CatalogException("FORBIDDEN", "Vous n'êtes pas propriétaire de cette collection", HttpStatus.FORBIDDEN);
        }

        // Vérifier que l'asset existe
        if (!assetRepository.existsById(assetId)) {
            throw new CatalogException("ASSET_NOT_FOUND", "Asset introuvable", HttpStatus.BAD_REQUEST);
        }

        // Idempotence : ne pas échouer si le lieu est déjà dans la collection
        if (collectionPlaceRepository.existsByCollectionIdAndAssetId(collectionId, assetId)) {
            return; // Déjà présent, succès silencieux
        }

        CollectionPlaceEntity collectionPlace = new CollectionPlaceEntity();
        collectionPlace.setCollectionId(collectionId);
        collectionPlace.setAssetId(assetId);
        collectionPlace.setAddedAt(Instant.now());
        collectionPlaceRepository.save(collectionPlace);

        // Mettre à jour updated_at de la collection
        collection.setUpdatedAt(Instant.now());
        collectionRepository.save(collection);

        outbox.append("catalog.collection.place_added", collectionId.toString(), actorId, correlationId,
                java.util.Map.of("collectionId", collectionId.toString(), "assetId", assetId.toString()));
    }

    @Transactional
    public void removePlace(UUID collectionId, UUID assetId, UUID requesterId, String correlationId, String actorId) {
        CollectionEntity collection = getRequired(collectionId);
        
        // IDOR Protection: vérifier la propriété
        if (!collection.getUserId().equals(requesterId)) {
            throw new CatalogException("FORBIDDEN", "Vous n'êtes pas propriétaire de cette collection", HttpStatus.FORBIDDEN);
        }

        CollectionPlaceEntity.CollectionPlaceId id = new CollectionPlaceEntity.CollectionPlaceId(collectionId, assetId);
        
        if (collectionPlaceRepository.existsById(id)) {
            collectionPlaceRepository.deleteById(id);
            
            // Mettre à jour updated_at de la collection
            collection.setUpdatedAt(Instant.now());
            collectionRepository.save(collection);

            outbox.append("catalog.collection.place_removed", collectionId.toString(), actorId, correlationId,
                    java.util.Map.of("collectionId", collectionId.toString(), "assetId", assetId.toString()));
        }
    }

    // ─── HELPERS ────────────────────────────────────────────────────────────────

    private CollectionEntity getRequired(UUID collectionId) {
        return collectionRepository.findById(collectionId)
                .orElseThrow(() -> new CatalogException("COLLECTION_NOT_FOUND", "Collection introuvable", HttpStatus.NOT_FOUND));
    }

    // ─── NESTED CLASSES ─────────────────────────────────────────────────────────

    public record CollectionWithAssets(CollectionEntity collection, List<CatalogAsset> assets) {}
    
    public record CollectionSummary(UUID id, String title, UUID coverAssetId, long placeCount) {}
}
