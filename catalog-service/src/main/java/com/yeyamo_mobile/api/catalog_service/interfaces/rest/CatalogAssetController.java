package com.yeyamo_mobile.api.catalog_service.interfaces.rest;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;
import java.time.Instant;
import com.yeyamo_mobile.api.catalog_service.application.AdminCatalogAssetService;
import com.yeyamo_mobile.api.catalog_service.domain.model.AssetStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import com.yeyamo_mobile.api.catalog_service.application.CatalogAssetService;
import com.yeyamo_mobile.api.catalog_service.domain.model.AssetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController @RequestMapping("/api/v1/catalog/assets") @Tag(name="Catalog assets")
public class CatalogAssetController {
    private final CatalogAssetService service;private final AdminCatalogAssetService adminService;
    public CatalogAssetController(CatalogAssetService service,AdminCatalogAssetService adminService){this.service=service;this.adminService=adminService;}
    @GetMapping("/manage") @PreAuthorize("hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')") public Page<AdminCatalogAssetResponse>manageList(@RequestParam(required=false)String search,@RequestParam(required=false)AssetType type,@RequestParam(required=false)AssetStatus status,@RequestParam(required=false)String regionId,@RequestParam(required=false)String categoryId,@RequestParam(required=false)String source,@RequestParam(required=false)Instant createdFrom,@RequestParam(required=false)Instant createdTo,Pageable pageable){return adminService.search(search,type,status,regionId,categoryId,source,createdFrom,createdTo,pageable);}
    @GetMapping("/{id}") public CatalogAssetResponse get(@PathVariable UUID id){return CatalogAssetResponse.from(service.get(id));}
    @GetMapping("/manage/{id}") @Operation(summary="Read any asset status for management",security=@SecurityRequirement(name="bearerAuth")) public CatalogAssetResponse manage(@PathVariable UUID id){return CatalogAssetResponse.from(service.getForManagement(id));}
    @GetMapping("/slug/{slug}") public CatalogAssetResponse bySlug(@PathVariable String slug){return CatalogAssetResponse.from(service.getBySlug(slug));}
    @GetMapping(params="!page") public List<CatalogAssetResponse> search(@RequestParam(required=false) AssetType type,
            @RequestParam(required=false) String regionCode,@RequestParam(required=false) String categoryCode,
            @RequestParam(required=false) String q,@RequestParam(defaultValue="50") @Min(1) @Max(100) int limit){
        return service.search(type,regionCode,categoryCode,q,limit).stream().map(CatalogAssetResponse::from).toList();
    }
    @GetMapping(params="page") @PreAuthorize("hasAnyRole('EDITOR','ADMIN','SUPER_ADMIN')")
    public Page<AdminCatalogAssetResponse> adminSearch(@RequestParam int page,@RequestParam(defaultValue="20")int size,
            @RequestParam(required=false)String search,@RequestParam(required=false)AssetType type,
            @RequestParam(required=false)AssetStatus status,@RequestParam(required=false)String regionId,
            @RequestParam(required=false)String categoryId,@RequestParam(required=false)String source,
            @RequestParam(required=false)Instant createdFrom,@RequestParam(required=false)Instant createdTo,
            @RequestParam(defaultValue="createdAt,desc")String sort){
        String[] parts=sort.split(",",2);Sort.Direction direction=parts.length>1&&"asc".equalsIgnoreCase(parts[1])?Sort.Direction.ASC:Sort.Direction.DESC;
        return adminService.search(search,type,status,regionId,categoryId,source,createdFrom,createdTo,
                PageRequest.of(Math.max(0,page),Math.max(1,Math.min(100,size)),Sort.by(direction,parts[0])));
    }
    @GetMapping("/nearby") public List<CatalogAssetResponse> nearby(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
            @RequestParam(defaultValue="5") @DecimalMin("0.1") @DecimalMax("100") double radiusKm,
            @RequestParam(required=false) AssetType type,@RequestParam(required=false) String categoryCode,
            @RequestParam(defaultValue="50") @Min(1) @Max(100) int limit){
        return service.nearby(lat,lng,radiusKm,type,categoryCode,limit).stream().map(CatalogAssetResponse::from).toList();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Operation(summary="Create a destination, place or experience",security=@SecurityRequirement(name="bearerAuth"))
    public CatalogAssetResponse create(@Valid @RequestBody CatalogAssetRequest r,
            @RequestHeader(value="X-Correlation-Id",required=false) String correlationId,Authentication auth){
        return CatalogAssetResponse.from(service.create(r.type(),r.ownerId(),r.name(),r.slug(),r.description(),
                r.categoryCode(),r.countryCode(),r.regionCode(),r.city(),r.district(),r.address(),r.latitude(),r.longitude(),
                correlationId,auth.getName()));
    }
    @PutMapping("/{id}") @Operation(summary="Update a catalog asset",security=@SecurityRequirement(name="bearerAuth")) public CatalogAssetResponse update(@PathVariable UUID id,@Valid @RequestBody CatalogAssetRequest r,
            @RequestHeader(value="X-Correlation-Id",required=false) String correlationId,Authentication auth){
        return CatalogAssetResponse.from(service.update(id,r.name(),r.slug(),r.description(),r.categoryCode(),
                r.countryCode(),r.regionCode(),r.city(),r.district(),r.address(),r.latitude(),r.longitude(),correlationId,auth.getName()));
    }
    @PatchMapping("/{id}/status") @Operation(summary="Change catalog workflow status",security=@SecurityRequirement(name="bearerAuth")) public CatalogAssetResponse status(@PathVariable UUID id,
            @Valid @RequestBody StatusChangeRequest r,
            @RequestHeader(value="X-Correlation-Id",required=false) String correlationId,Authentication auth){
        return CatalogAssetResponse.from(service.changeStatus(id,r.status(),correlationId,auth.getName()));
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Operation(summary="Soft-delete a catalog asset",security=@SecurityRequirement(name="bearerAuth"))
    public void delete(@PathVariable UUID id,@RequestHeader(value="X-Correlation-Id",required=false)String correlationId,Authentication auth){service.delete(id,correlationId,auth.getName());}
}
