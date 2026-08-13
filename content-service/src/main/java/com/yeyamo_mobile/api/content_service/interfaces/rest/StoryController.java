package com.yeyamo_mobile.api.content_service.interfaces.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.yeyamo_mobile.api.content_service.application.StoryService;
import com.yeyamo_mobile.api.content_service.infrastructure.persistence.StoryEntity;
import com.yeyamo_mobile.shared.geography.GeographicFields;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/stories")
@Tag(name = "Stories", description = "Contenus éphémères (24h)")
public class StoryController {
    
    private final StoryService service;

    public StoryController(StoryService service) {
        this.service = service;
    }

    // ─── GET /api/v1/stories ────────────────────────────────────────────────────

    @GetMapping
    @Operation(
        summary = "Liste des stories actives", 
        description = "Récupère les stories actives (non expirées) des utilisateurs suivis par l'utilisateur connecté",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<StoryResponse> getActiveStories(Authentication auth) {
        String userId = auth.getName();
        
        return service.getActiveStoriesForUser(userId).stream()
                .map(StoryResponse::from)
                .toList();
    }

    // ─── GET /api/v1/stories/{id} ───────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(
        summary = "Détail d'une story", 
        description = "Récupère une story par son ID. Retourne 404 si la story est expirée ou supprimée",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public StoryResponse getStory(@PathVariable UUID id, Authentication auth) {
        StoryEntity story = service.getActiveStory(id);
        
        // Compter les vues
        long viewCount = service.getViewCount(id);
        
        return new StoryResponse(
            story.getId(),
            story.getAuthorId(),
            story.getMediaId(),
            story.getCaption(),
            story.getDurationSeconds(),
            story.getCreatedAt(),
            story.getExpiresAt(),
            viewCount,
            false // viewedByMe peut être ajouté si nécessaire
        );
    }

    // ─── POST /api/v1/stories/{id}/view ─────────────────────────────────────────

    @PostMapping("/{id}/view")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Enregistrer une vue", 
        description = "Marque une story comme vue par l'utilisateur connecté. Idempotent : une deuxième vue ne duplique pas l'entrée",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public void recordView(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication auth) {
        
        String viewerId = auth.getName();
        service.recordView(id, viewerId, correlationId);
    }

    // ─── POST /api/v1/stories ───────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Créer une story", 
        description = "Crée une nouvelle story qui expirera automatiquement après 24 heures",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public StoryResponse create(
            @Valid @RequestBody StoryRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication auth) {
        
        String authorId = auth.getName();
        int duration = request.durationSeconds() != null ? request.durationSeconds() : 15;
        
        StoryEntity story = service.create(
                authorId,
                request.mediaId(),
                request.caption(),
                duration,
                geography(request),
                correlationId
        );
        
        return StoryResponse.from(story);
    }

    // ─── DELETE /api/v1/stories/{id} ────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Supprimer une story", 
        description = "Supprime une story (soft delete). Seul l'auteur peut supprimer sa story",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public void delete(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication auth) {
        
        String authorId = auth.getName();
        service.delete(id, authorId, correlationId);
    }

    private GeographicFields geography(StoryRequest request) {
        if (request.countryCode() == null || request.countryCode().isBlank()) return null;
        GeographicFields geography = new GeographicFields(request.countryCode());
        geography.setLocation(request.adminLevel1Id(), request.adminLevel2Id(), request.cityId(), request.localityId());
        geography.setCoordinates(request.latitude(), request.longitude());
        geography.setLanguageCode(request.languageCode());
        return geography;
    }
}
