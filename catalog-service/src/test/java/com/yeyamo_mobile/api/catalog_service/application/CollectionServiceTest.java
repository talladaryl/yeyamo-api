package com.yeyamo_mobile.api.catalog_service.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import com.yeyamo_mobile.api.catalog_service.application.port.OutboxPort;
import com.yeyamo_mobile.api.catalog_service.domain.port.CatalogAssetRepository;
import com.yeyamo_mobile.api.catalog_service.infrastructure.persistence.CollectionEntity;
import com.yeyamo_mobile.api.catalog_service.infrastructure.persistence.CollectionPlaceEntity;
import com.yeyamo_mobile.api.catalog_service.infrastructure.persistence.SpringDataCollectionPlaceRepository;
import com.yeyamo_mobile.api.catalog_service.infrastructure.persistence.SpringDataCollectionRepository;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @Mock private SpringDataCollectionRepository collectionRepository;
    @Mock private SpringDataCollectionPlaceRepository collectionPlaceRepository;
    @Mock private CatalogAssetRepository assetRepository;
    @Mock private OutboxPort outbox;

    @InjectMocks private CollectionService service;

    private String userId;
    private String otherUserId;
    private UUID assetId;
    private CollectionEntity collection;

    @BeforeEach
    void setUp() {
        userId = "42";
        otherUserId = "99";
        assetId = UUID.randomUUID();
        
        collection = new CollectionEntity();
        collection.setId(UUID.randomUUID());
        collection.setUserId(userId);
        collection.setTitle("Ma collection");
        collection.setDescription("Description test");
        collection.setPublic(false);
        collection.setCreatedAt(Instant.now());
        collection.setUpdatedAt(Instant.now());
    }

    // ─── TESTS CRÉATION ─────────────────────────────────────────────────────────

    @Test
    void shouldCreateCollection() {
        when(collectionRepository.save(any(CollectionEntity.class))).thenReturn(collection);

        CollectionEntity result = service.create(userId, "Titre", "Description", true, null, "corr-1", "user1");

        assertNotNull(result);
        verify(collectionRepository).save(any(CollectionEntity.class));
        verify(outbox).append(eq("catalog.collection.created"), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldFailCreateWithInvalidCoverAsset() {
        UUID invalidAssetId = UUID.randomUUID();
        when(assetRepository.existsById(invalidAssetId)).thenReturn(false);

        CatalogException ex = assertThrows(CatalogException.class, 
            () -> service.create(userId, "Titre", "Desc", false, invalidAssetId, "corr-1", "user1"));

        assertEquals("ASSET_NOT_FOUND", ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // ─── TESTS LECTURE ──────────────────────────────────────────────────────────

    @Test
    void shouldGetMyCollections() {
        Page<CollectionEntity> page = new PageImpl<>(List.of(collection));
        when(collectionRepository.findByUserIdOrderByUpdatedAtDesc(userId, PageRequest.of(0, 20))).thenReturn(page);

        Page<CollectionEntity> result = service.getMyCollections(userId, PageRequest.of(0, 20));

        assertEquals(1, result.getContent().size());
        assertEquals(collection.getId(), result.getContent().get(0).getId());
    }

    @Test
    void shouldGetPublicCollections() {
        collection.setPublic(true);
        Page<CollectionEntity> page = new PageImpl<>(List.of(collection));
        when(collectionRepository.findByIsPublicTrueOrderByUpdatedAtDesc(PageRequest.of(0, 20))).thenReturn(page);

        Page<CollectionEntity> result = service.getPublicCollections(PageRequest.of(0, 20));

        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).isPublic());
    }

    @Test
    void shouldGetPublicCollectionAsOwner() {
        collection.setPublic(true);
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));
        when(collectionPlaceRepository.findByCollectionIdOrderByAddedAtDesc(eq(collection.getId()), any()))
                .thenReturn(Page.empty());
        when(assetRepository.findAllById(anyList())).thenReturn(List.of());

        CollectionService.CollectionWithAssets result = service.getCollection(collection.getId(), userId);

        assertNotNull(result);
        assertEquals(collection.getId(), result.collection().getId());
    }

    @Test
    void shouldGetPublicCollectionAsStranger() {
        collection.setPublic(true);
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));
        when(collectionPlaceRepository.findByCollectionIdOrderByAddedAtDesc(eq(collection.getId()), any()))
                .thenReturn(Page.empty());
        when(assetRepository.findAllById(anyList())).thenReturn(List.of());

        CollectionService.CollectionWithAssets result = service.getCollection(collection.getId(), otherUserId);

        assertNotNull(result);
        assertEquals(collection.getId(), result.collection().getId());
    }

    @Test
    void shouldReturn404ForPrivateCollectionAsStranger() {
        collection.setPublic(false);
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));

        CatalogException ex = assertThrows(CatalogException.class,
            () -> service.getCollection(collection.getId(), otherUserId));

        assertEquals("COLLECTION_NOT_FOUND", ex.getCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus()); // 404, pas 403
    }

    // ─── TESTS MODIFICATION ─────────────────────────────────────────────────────

    @Test
    void shouldUpdateCollectionAsOwner() {
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));
        when(collectionRepository.save(any(CollectionEntity.class))).thenReturn(collection);

        CollectionEntity result = service.update(collection.getId(), userId, "Nouveau titre", null, true, null, "corr-1", "user1");

        assertNotNull(result);
        verify(collectionRepository).save(any(CollectionEntity.class));
        verify(outbox).append(eq("catalog.collection.updated"), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldFailUpdateAsNonOwner() {
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));

        CatalogException ex = assertThrows(CatalogException.class,
            () -> service.update(collection.getId(), otherUserId, "Nouveau", null, null, null, "corr-1", "user2"));

        assertEquals("FORBIDDEN", ex.getCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(collectionRepository, never()).save(any());
    }

    // ─── TESTS SUPPRESSION ──────────────────────────────────────────────────────

    @Test
    void shouldDeleteCollectionAsOwner() {
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));

        assertDoesNotThrow(() -> service.delete(collection.getId(), userId, "corr-1", "user1"));

        verify(collectionRepository).deleteById(collection.getId());
        verify(outbox).append(eq("catalog.collection.deleted"), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldFailDeleteAsNonOwner() {
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));

        CatalogException ex = assertThrows(CatalogException.class,
            () -> service.delete(collection.getId(), otherUserId, "corr-1", "user2"));

        assertEquals("FORBIDDEN", ex.getCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(collectionRepository, never()).deleteById(any());
    }

    // ─── TESTS AJOUT LIEU ───────────────────────────────────────────────────────

    @Test
    void shouldAddPlaceToCollection() {
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));
        when(assetRepository.existsById(assetId)).thenReturn(true);
        when(collectionPlaceRepository.findByCollectionIdAndAssetId(collection.getId(), assetId))
                .thenReturn(Optional.empty());
        when(collectionRepository.save(any(CollectionEntity.class))).thenReturn(collection);

        assertDoesNotThrow(() -> service.addPlace(collection.getId(), assetId, userId, true, "À visiter",
                "corr-1", "user1"));

        verify(collectionPlaceRepository).save(any(CollectionPlaceEntity.class));
        verify(outbox).append(eq("catalog.collection.place_added"), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldBeIdempotentWhenAddingExistingPlace() {
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));
        when(assetRepository.existsById(assetId)).thenReturn(true);
        CollectionPlaceEntity existing = new CollectionPlaceEntity();
        existing.setCollectionId(collection.getId());
        existing.setAssetId(assetId);
        when(collectionPlaceRepository.findByCollectionIdAndAssetId(collection.getId(), assetId))
                .thenReturn(Optional.of(existing));

        // Ne doit pas lever d'exception
        assertDoesNotThrow(() -> service.addPlace(collection.getId(), assetId, userId, null, null,
                "corr-1", "user1"));

        // Ne doit pas sauvegarder ni publier d'événement
        verify(collectionPlaceRepository).save(existing);
        verify(outbox, never()).append(anyString(), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldFailAddPlaceAsNonOwner() {
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));

        CatalogException ex = assertThrows(CatalogException.class,
            () -> service.addPlace(collection.getId(), assetId, otherUserId, null, null,
                    "corr-1", "user2"));

        assertEquals("FORBIDDEN", ex.getCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(collectionPlaceRepository, never()).save(any());
    }

    @Test
    void shouldFailAddNonExistentAsset() {
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));
        when(assetRepository.existsById(assetId)).thenReturn(false);

        CatalogException ex = assertThrows(CatalogException.class,
            () -> service.addPlace(collection.getId(), assetId, userId, null, null,
                    "corr-1", "user1"));

        assertEquals("ASSET_NOT_FOUND", ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // ─── TESTS RETRAIT LIEU ─────────────────────────────────────────────────────

    @Test
    void shouldRemovePlaceFromCollection() {
        CollectionPlaceEntity.CollectionPlaceId id = new CollectionPlaceEntity.CollectionPlaceId(collection.getId(), assetId);
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));
        when(collectionPlaceRepository.existsById(id)).thenReturn(true);
        when(collectionRepository.save(any(CollectionEntity.class))).thenReturn(collection);

        assertDoesNotThrow(() -> service.removePlace(collection.getId(), assetId, userId, "corr-1", "user1"));

        verify(collectionPlaceRepository).deleteById(id);
        verify(outbox).append(eq("catalog.collection.place_removed"), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldFailRemovePlaceAsNonOwner() {
        when(collectionRepository.findById(collection.getId())).thenReturn(Optional.of(collection));

        CatalogException ex = assertThrows(CatalogException.class,
            () -> service.removePlace(collection.getId(), assetId, otherUserId, "corr-1", "user2"));

        assertEquals("FORBIDDEN", ex.getCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(collectionPlaceRepository, never()).deleteById(any());
    }
}
