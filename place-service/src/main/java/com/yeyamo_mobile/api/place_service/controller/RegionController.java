package com.yeyamo_mobile.api.place_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import com.yeyamo_mobile.api.place_service.dto.ReferenceStatusRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.place_service.dto.PlaceSummaryResponse;
import com.yeyamo_mobile.api.place_service.dto.RegionRequest;
import com.yeyamo_mobile.api.place_service.dto.RegionResponse;
import com.yeyamo_mobile.api.place_service.service.PlaceService;
import com.yeyamo_mobile.api.place_service.service.RegionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/regions")
public class RegionController {

    private final RegionService regionService;
    private final PlaceService placeService;

    public RegionController(RegionService regionService, PlaceService placeService) {
        this.regionService = regionService;
        this.placeService = placeService;
    }

    @GetMapping
    public List<RegionResponse> listRegions() {
        return regionService.listRegions();
    }

    @GetMapping("/{slug}")
    public RegionResponse getBySlug(@PathVariable String slug) {
        return regionService.getBySlug(slug);
    }

    @GetMapping("/{slug}/places")
    public List<PlaceSummaryResponse> placesByRegion(@PathVariable String slug) {
        return placeService.findByRegionSlug(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegionResponse create(@Valid @RequestBody RegionRequest request) {
        return regionService.create(request);
    }

    @PutMapping("/{id}")
    public RegionResponse update(@PathVariable Long id, @Valid @RequestBody RegionRequest request) {
        return regionService.update(id, request);
    }
    @PatchMapping("/{id}/status") @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") public RegionResponse status(@PathVariable Long id,@RequestBody ReferenceStatusRequest request){return regionService.setActive(id,request.active());}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('SUPER_ADMIN')") public void delete(@PathVariable Long id){regionService.delete(id);}
}
