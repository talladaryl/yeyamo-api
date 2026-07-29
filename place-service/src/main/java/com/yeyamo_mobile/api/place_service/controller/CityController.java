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

import com.yeyamo_mobile.api.place_service.dto.CityRequest;
import com.yeyamo_mobile.api.place_service.dto.CityResponse;
import com.yeyamo_mobile.api.place_service.dto.PlaceSummaryResponse;
import com.yeyamo_mobile.api.place_service.service.CityService;
import com.yeyamo_mobile.api.place_service.service.PlaceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/cities")
public class CityController {

    private final CityService cityService;
    private final PlaceService placeService;

    public CityController(CityService cityService, PlaceService placeService) {
        this.cityService = cityService;
        this.placeService = placeService;
    }

    @GetMapping("/{id}/places")
    public List<PlaceSummaryResponse> placesByCity(@PathVariable Long id) {
        return placeService.findByCityId(id);
    }

    @GetMapping("/region/{regionId}")
    public List<CityResponse> listByRegion(@PathVariable Long regionId) {
        return cityService.listByRegion(regionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CityResponse create(@Valid @RequestBody CityRequest request) {
        return cityService.create(request);
    }

    @PutMapping("/{id}")
    public CityResponse update(@PathVariable Long id, @Valid @RequestBody CityRequest request) {
        return cityService.update(id, request);
    }
    @PatchMapping("/{id}/status") @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") public CityResponse status(@PathVariable Long id,@RequestBody ReferenceStatusRequest request){return cityService.setActive(id,request.active());}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('SUPER_ADMIN')") public void delete(@PathVariable Long id){cityService.delete(id);}
}
