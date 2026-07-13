package com.yeyamo_mobile.api.catalog_service.interfaces.rest;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.yeyamo_mobile.api.catalog_service.application.CatalogException;
import com.yeyamo_mobile.api.catalog_service.application.CatalogReferenceService;
import com.yeyamo_mobile.api.catalog_service.domain.model.ReferenceType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/catalog/{kind:regions|cities|categories}")
@Tag(name = "Catalog references", description = "Regions, cities and category taxonomy")
public class CatalogReferenceController {
    private final CatalogReferenceService service;
    public CatalogReferenceController(CatalogReferenceService service) { this.service = service; }

    @GetMapping
    public List<CatalogReferenceResponse> list(@PathVariable String kind,
            @RequestParam(required = false) String parentCode,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "100") @Min(1) @Max(200) int limit) {
        return service.list(type(kind), parentCode, activeOnly, limit).stream().map(CatalogReferenceResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CatalogReferenceResponse get(@PathVariable String kind, @PathVariable UUID id) {
        var result = service.getPublic(id);
        ensureType(kind, result.getType());
        return CatalogReferenceResponse.from(result);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a region, city or category", security = @SecurityRequirement(name = "bearerAuth"))
    public CatalogReferenceResponse create(@PathVariable String kind, @Valid @RequestBody CatalogReferenceRequest request,
            @RequestHeader(value="X-Correlation-Id",required=false)String correlationId,Authentication authentication) {
        return CatalogReferenceResponse.from(service.create(type(kind), request.code(), request.name(),
                request.parentCode(), request.countryCode(), request.description(),correlationId,authentication.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a region, city or category", security = @SecurityRequirement(name = "bearerAuth"))
    public CatalogReferenceResponse update(@PathVariable String kind, @PathVariable UUID id,
            @Valid @RequestBody CatalogReferenceRequest request,@RequestHeader(value="X-Correlation-Id",required=false)String correlationId,Authentication authentication) {
        ensureType(kind, service.get(id).getType());
        return CatalogReferenceResponse.from(service.update(id, request.name(), request.parentCode(),
                request.countryCode(), request.description(),correlationId,authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a catalog reference", security = @SecurityRequirement(name = "bearerAuth"))
    public void deactivate(@PathVariable String kind, @PathVariable UUID id,@RequestHeader(value="X-Correlation-Id",required=false)String correlationId,Authentication authentication) {
        ensureType(kind, service.get(id).getType());
        service.setActive(id, false,correlationId,authentication.getName());
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Reactivate a catalog reference", security = @SecurityRequirement(name = "bearerAuth"))
    public CatalogReferenceResponse activate(@PathVariable String kind, @PathVariable UUID id,@RequestHeader(value="X-Correlation-Id",required=false)String correlationId,Authentication authentication) {
        ensureType(kind, service.get(id).getType());
        return CatalogReferenceResponse.from(service.setActive(id, true,correlationId,authentication.getName()));
    }

    private ReferenceType type(String kind) {
        return switch (kind.toLowerCase(Locale.ROOT)) {
            case "regions" -> ReferenceType.REGION;
            case "cities" -> ReferenceType.CITY;
            case "categories" -> ReferenceType.CATEGORY;
            default -> throw new CatalogException("INVALID_REFERENCE_TYPE", "Unsupported reference type");
        };
    }
    private void ensureType(String kind, ReferenceType actual) {
        if (type(kind) != actual) throw new CatalogException("CATALOG_REFERENCE_NOT_FOUND", "Catalog reference not found");
    }
}
