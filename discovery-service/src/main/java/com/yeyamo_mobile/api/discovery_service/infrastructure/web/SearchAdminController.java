package com.yeyamo_mobile.api.discovery_service.infrastructure.web;

import com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin.CultureIndexManager;
import com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin.SearchAdminDtos;
import com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin.SearchAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin endpoints for search management.
 * Extended to support per-index rebuild for culture/artisan indexes.
 */
@RestController
@RequestMapping("/api/v1/admin/search")
@Tag(name = "Search Admin", description = "Admin endpoints for search configuration and maintenance")
@SecurityRequirement(name = "bearerAuth")
public class SearchAdminController {

    private final SearchAdminService adminService;
    private final CultureIndexManager cultureIndexManager;

    public SearchAdminController(SearchAdminService adminService, CultureIndexManager cultureIndexManager) {
        this.adminService       = adminService;
        this.cultureIndexManager = cultureIndexManager;
    }

    // -------------------------------------------------------------------------
    // Overview
    // -------------------------------------------------------------------------

    @GetMapping("/overview")
    @Operation(summary = "Cluster health and index overview")
    public ResponseEntity<SearchAdminDtos.Overview> overview() {
        return ResponseEntity.ok(adminService.overview());
    }

    @GetMapping("/indexes")
    @Operation(summary = "List all indexes")
    public ResponseEntity<List<SearchAdminDtos.IndexInfo>> indexes() {
        return ResponseEntity.ok(adminService.indexes());
    }

    // -------------------------------------------------------------------------
    // Reindex — full and per-index
    // -------------------------------------------------------------------------

    /** Full reindex of the primary yeyamo-discovery index. */
    @PostMapping("/reindex")
    @Operation(summary = "Trigger full reindex")
    public ResponseEntity<SearchAdminDtos.ReindexResponse> reindex(Authentication auth) {
        return ResponseEntity.accepted().body(adminService.reindex(auth.getName()));
    }

    /**
     * Rebuild a single named culture/artisan index.
     * Valid names: {@code culture_contents_v1}, {@code artworks_v1},
     *              {@code artisans_v1}, {@code languages_v1}.
     */
    @PostMapping("/reindex/{indexName}")
    @Operation(summary = "Trigger reindex for a specific culture index")
    public ResponseEntity<Void> reindexCultureIndex(
            Authentication auth,
            @PathVariable String indexName) {

        if (!CultureIndexManager.ALL_CULTURE_INDEXES.contains(indexName)) {
            return ResponseEntity.badRequest().build();
        }
        cultureIndexManager.reindex(indexName);
        return ResponseEntity.accepted().build();
    }

    /** Ensure all culture indexes exist (creates if missing). */
    @PostMapping("/indexes/culture/ensure")
    @Operation(summary = "Create culture indexes if missing")
    public ResponseEntity<Void> ensureCultureIndexes() {
        cultureIndexManager.ensureIndexes();
        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------
    // Synonyms
    // -------------------------------------------------------------------------

    @GetMapping("/synonyms")
    @Operation(summary = "List search synonyms")
    public ResponseEntity<List<SearchAdminDtos.SynonymResponse>> synonyms() {
        return ResponseEntity.ok(adminService.synonyms());
    }

    @PostMapping("/synonyms")
    @Operation(summary = "Create search synonym")
    public ResponseEntity<SearchAdminDtos.SynonymResponse> createSynonym(
            Authentication auth,
            @RequestBody SearchAdminDtos.SynonymRequest request) {
        return ResponseEntity.ok(adminService.createSynonym(auth.getName(), request));
    }

    // -------------------------------------------------------------------------
    // Ranking
    // -------------------------------------------------------------------------

    @GetMapping("/ranking")
    @Operation(summary = "Get active ranking config")
    public ResponseEntity<SearchAdminDtos.RankingResponse> ranking() {
        return ResponseEntity.ok(adminService.ranking());
    }

    @PostMapping("/ranking")
    @Operation(summary = "Set ranking weights")
    public ResponseEntity<SearchAdminDtos.RankingResponse> ranking(
            Authentication auth,
            @RequestBody SearchAdminDtos.RankingRequest request) {
        return ResponseEntity.ok(adminService.ranking(auth.getName(), request));
    }

    // -------------------------------------------------------------------------
    // Zero-result queries
    // -------------------------------------------------------------------------

    @GetMapping("/zero-results")
    @Operation(summary = "Queries that returned zero results")
    public ResponseEntity<?> zeroResults(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.zeroResults(PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "occurrences"))));
    }
}
