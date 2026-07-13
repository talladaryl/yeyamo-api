package com.yeyamo_mobile.api.catalog_service.interfaces.rest;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.catalog_service.application.CatalogAssetService;
import com.yeyamo_mobile.api.catalog_service.domain.model.AssetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController @RequestMapping("/api/v1/catalog/assets")
public class CatalogAssetController {
    private final CatalogAssetService service;
    public CatalogAssetController(CatalogAssetService service){this.service=service;}
    @GetMapping("/{id}") public CatalogAssetResponse get(@PathVariable UUID id){return CatalogAssetResponse.from(service.get(id));}
    @GetMapping("/slug/{slug}") public CatalogAssetResponse bySlug(@PathVariable String slug){return CatalogAssetResponse.from(service.getBySlug(slug));}
    @GetMapping public List<CatalogAssetResponse> search(@RequestParam(required=false) AssetType type,
            @RequestParam(required=false) String regionCode,@RequestParam(required=false) String categoryCode,
            @RequestParam(required=false) String q,@RequestParam(defaultValue="50") @Min(1) @Max(100) int limit){
        return service.search(type,regionCode,categoryCode,q,limit).stream().map(CatalogAssetResponse::from).toList();
    }
    @GetMapping("/nearby") public List<CatalogAssetResponse> nearby(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
            @RequestParam(defaultValue="5") @DecimalMin("0.1") @DecimalMax("100") double radiusKm,
            @RequestParam(required=false) AssetType type,@RequestParam(required=false) String categoryCode,
            @RequestParam(defaultValue="50") @Min(1) @Max(100) int limit){
        return service.nearby(lat,lng,radiusKm,type,categoryCode,limit).stream().map(CatalogAssetResponse::from).toList();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CatalogAssetResponse create(@Valid @RequestBody CatalogAssetRequest r,
            @RequestHeader(value="X-Correlation-Id",required=false) String correlationId,Authentication auth){
        return CatalogAssetResponse.from(service.create(r.type(),r.ownerId(),r.name(),r.slug(),r.description(),
                r.categoryCode(),r.regionCode(),r.city(),r.district(),r.address(),r.latitude(),r.longitude(),
                correlationId,auth.getName()));
    }
    @PutMapping("/{id}") public CatalogAssetResponse update(@PathVariable UUID id,@Valid @RequestBody CatalogAssetRequest r,
            @RequestHeader(value="X-Correlation-Id",required=false) String correlationId,Authentication auth){
        return CatalogAssetResponse.from(service.update(id,r.name(),r.slug(),r.description(),r.categoryCode(),
                r.regionCode(),r.city(),r.district(),r.address(),r.latitude(),r.longitude(),correlationId,auth.getName()));
    }
    @PatchMapping("/{id}/status") public CatalogAssetResponse status(@PathVariable UUID id,
            @Valid @RequestBody StatusChangeRequest r,
            @RequestHeader(value="X-Correlation-Id",required=false) String correlationId,Authentication auth){
        return CatalogAssetResponse.from(service.changeStatus(id,r.status(),correlationId,auth.getName()));
    }
}
