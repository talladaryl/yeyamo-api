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
    public CollectionEntity create(String userId, String title, String description, boolean isPublic,
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
                java.util.Map.of("collectionId", saved.getId().toString(), "userId", userId, "title", title));
        
        return saved;
    }

    @Transactional
    public CollectionEntity update(UUID collectionId, String requesterId, String title, String description,
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
    public void delete(UUID collectionId, String requesterId, String correlationId, String actorId) {
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
    public Page<CollectionEntity> getMyCollections(String userId, Pageable pageable) {
        return collectionRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CollectionEntity> getPublicCollections(Pageable pageable) {
        return collectionRepository.findByIsPublicTrueOrderByUpdatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public CollectionWithAssets getCollection(UUID collectionId, String requesterId) {
        CollectionEntity collection = getRequired(collectionId);
        
        // Vérification de visibilité : public OU propriétaire
        if (!collection.isPublic() && !collection.getUserId().equals(requesterId)) {
            // Retourner 404 (pas 403) pour ne pas révéler l'existence d'une collection privée
            throw new CatalogException("COLLECTION_NOT_FOUND", "Collection introuvable", HttpStatus.NOT_FOUND);
        }

        // Charger les assets de la collection
        List<CollectionPlaceEntity> items =
                collectionPlaceRepository.findByCollectionIdOrderByAddedAtDesc(collectionId, Pageable.unpaged())
                        .getContent();
        List<UUID> assetIds = items.stream().map(CollectionPlaceEntity::getAssetId).toList();
        List<CatalogAsset> assets = assetRepository.findAllById(assetIds);
        
        return new CollectionWithAssets(collection, assets, items);
    }

    @Transactional(readOnly = true)
    public List<CollectionSummary> getSummaries(String userId) {
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
    public void addPlace(UUID collectionId, UUID assetId, String requesterId, Boolean priority, String note,
            String correlationId, String actorId) {
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
        var existing = collectionPlaceRepository.findByCollectionIdAndAssetId(collectionId, assetId);
        if (existing.isPresent()) {
            CollectionPlaceEntity item = existing.get();
            if (priority != null) item.setPriority(priority);
            if (note != null) item.setNote(normalizeNote(note));
            collectionPlaceRepository.save(item);
            return;
        }

        CollectionPlaceEntity collectionPlace = new CollectionPlaceEntity();
        collectionPlace.setCollectionId(collectionId);
        collectionPlace.setAssetId(assetId);
        collectionPlace.setAddedAt(Instant.now());
        collectionPlace.setPriority(Boolean.TRUE.equals(priority));
        collectionPlace.setNote(normalizeNote(note));
        collectionPlaceRepository.save(collectionPlace);

        // Mettre à jour updated_at de la collection
        collection.setUpdatedAt(Instant.now());
        collectionRepository.save(collection);

        outbox.append("catalog.collection.place_added", collectionId.toString(), actorId, correlationId,
                java.util.Map.of("collectionId", collectionId.toString(), "assetId", assetId.toString()));
    }

    @Transactional
    public void updatePlace(UUID collectionId, UUID assetId, String requesterId, Boolean priority, Integer displayOrder, String note,
            String correlationId, String actorId) {
        CollectionEntity collection = getRequired(collectionId);
        if (!collection.getUserId().equals(requesterId)) {
            throw new CatalogException("FORBIDDEN", "Vous n'êtes pas propriétaire de cette collection",
                    HttpStatus.FORBIDDEN);
        }
        CollectionPlaceEntity item = collectionPlaceRepository
                .findByCollectionIdAndAssetId(collectionId, assetId)
                .orElseThrow(() -> new CatalogException(
                        "COLLECTION_ITEM_NOT_FOUND", "Élément de collection introuvable", HttpStatus.NOT_FOUND));
        if (priority != null) item.setPriority(priority);
        if (displayOrder != null) item.setDisplayOrder(Math.max(0, displayOrder));
        if (note != null) item.setNote(normalizeNote(note));
        collectionPlaceRepository.save(item);
        collection.setUpdatedAt(Instant.now());
        collectionRepository.save(collection);
        outbox.append("catalog.collection.place_updated", collectionId.toString(), actorId, correlationId,
                java.util.Map.of("collectionId", collectionId.toString(), "assetId", assetId.toString()));
    }

    @Transactional
    public void removePlace(UUID collectionId, UUID assetId, String requesterId, String correlationId, String actorId) {
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

    private String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    // ─── NESTED CLASSES ─────────────────────────────────────────────────────────

    public record CollectionWithAssets(
            CollectionEntity collection,
            List<CatalogAsset> assets,
            List<CollectionPlaceEntity> items) {}
    
    public record CollectionSummary(UUID id, String title, UUID coverAssetId, long placeCount) {}
}
