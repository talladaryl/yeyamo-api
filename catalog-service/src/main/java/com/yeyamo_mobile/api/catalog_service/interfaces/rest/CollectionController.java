package com.yeyamo_mobile.api.catalog_service.interfaces.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.yeyamo_mobile.api.catalog_service.application.CollectionService;
import com.yeyamo_mobile.api.catalog_service.infrastructure.persistence.CollectionEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/collections")
@Tag(name = "Collections", description = "Listes personnalisées d'assets (lieux, expériences)")
@SecurityRequirement(name = "bearerAuth")
public class CollectionController {
    
    private final CollectionService service;

    public CollectionController(CollectionService service) {
        this.service = service;
    }

    // ─── MES COLLECTIONS ────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Lister mes collections", description = "Récupère toutes les collections de l'utilisateur connecté, paginées et triées par date de modification décroissante")
    public Page<CollectionResponse> getMyCollections(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication auth) {
        
        String userId = auth.getName();
        Page<CollectionEntity> collections = service.getMyCollections(userId, PageRequest.of(page, size));
        return collections.map(CollectionResponse::from);
    }

    // ─── COLLECTIONS PUBLIQUES ──────────────────────────────────────────────────

    @GetMapping("/public")
    @Operation(summary = "Lister les collections publiques", description = "Récupère les collections publiques d'autres utilisateurs, paginées et triées par date de modification décroissante")
    public Page<CollectionResponse> getPublicCollections(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        
        Page<CollectionEntity> collections = service.getPublicCollections(PageRequest.of(page, size));
        return collections.map(CollectionResponse::from);
    }

    // ─── DÉTAIL COLLECTION ──────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une collection avec ses lieux", description = "Retourne 404 si la collection est privée et que l'utilisateur n'est pas le propriétaire (pour ne pas révéler l'existence)")
    public CollectionResponse getCollection(@PathVariable UUID id, Authentication auth) {
        String userId = auth.getName();
        CollectionService.CollectionWithAssets data = service.getCollection(id, userId);
        return CollectionResponse.fromWithAssets(data);
    }

    // ─── SUMMARIES (pour listes déroulantes) ───────────────────────────────────

    @GetMapping("/summaries")
    @Operation(summary = "Résumés de collections", description = "Version allégée des collections (id, titre, nombre de lieux, cover) pour les listes déroulantes 'ajouter à une collection'")
    public List<CollectionSummaryResponse> getSummaries(Authentication auth) {
        String userId = auth.getName();
        return service.getSummaries(userId).stream()
                .map(CollectionSummaryResponse::from)
                .toList();
    }

    // ─── CRÉER COLLECTION ───────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une collection")
    public CollectionResponse create(
            @Valid @RequestBody CollectionRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication auth) {
        
        String userId = auth.getName();
        boolean isPublic = request.isPublic() != null ? request.isPublic() : false;
        
        CollectionEntity collection = service.create(
                userId,
                request.title(),
                request.description(),
                isPublic,
                request.coverAssetId(),
                correlationId,
                auth.getName()
        );
        
        return CollectionResponse.from(collection);
    }

    // ─── MODIFIER COLLECTION ────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une collection", description = "Propriétaire uniquement (403 sinon)")
    public CollectionResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody CollectionRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication auth) {
        
        String userId = auth.getName();
        
        CollectionEntity collection = service.update(
                id,
                userId,
                request.title(),
                request.description(),
                request.isPublic(),
                request.coverAssetId(),
                correlationId,
                auth.getName()
        );
        
        return CollectionResponse.from(collection);
    }

    // ─── SUPPRIMER COLLECTION ───────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une collection", description = "Propriétaire uniquement (403 sinon)")
    public void delete(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication auth) {
        
        String userId = auth.getName();
        service.delete(id, userId, correlationId, auth.getName());
    }

    // ─── AJOUTER LIEU À COLLECTION ──────────────────────────────────────────────

    @PostMapping("/places")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Ajouter un lieu à une collection", description = "Propriétaire uniquement. Idempotent : ne génère pas d'erreur si le lieu est déjà présent")
    public void addPlace(
            @Valid @RequestBody AddPlaceRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication auth) {
        
        String userId = auth.getName();
        service.addPlace(request.collectionId(), request.assetId(), userId, request.isPriority(), request.note(),
                correlationId, auth.getName());
    }

    @PatchMapping("/{collectionId}/places/{assetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Modifier la note ou la priorité d'un élément de collection",
            description = "Propriétaire uniquement. Les champs omis conservent leur valeur actuelle")
    public void updatePlace(
            @PathVariable UUID collectionId,
            @PathVariable UUID assetId,
            @Valid @RequestBody UpdateCollectionPlaceRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication auth) {
        service.updatePlace(collectionId, assetId, auth.getName(), request.isPriority(), request.note(),
                correlationId, auth.getName());
    }

    // ─── RETIRER LIEU DE COLLECTION ─────────────────────────────────────────────

    @DeleteMapping("/{collectionId}/places/{assetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retirer un lieu d'une collection", description = "Propriétaire uniquement (403 sinon)")
    public void removePlace(
            @PathVariable UUID collectionId,
            @PathVariable UUID assetId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication auth) {
        
        String userId = auth.getName();
        service.removePlace(collectionId, assetId, userId, correlationId, auth.getName());
    }

}
