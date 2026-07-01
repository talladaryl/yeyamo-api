package com.yeyamo_mobile.api.place_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.place_service.dto.PlaceRequest;
import com.yeyamo_mobile.api.place_service.dto.PlaceResponse;
import com.yeyamo_mobile.api.place_service.dto.PlaceSummaryResponse;
import com.yeyamo_mobile.api.place_service.service.PlaceService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/places")
@Validated
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping("/nearby")
    public List<PlaceSummaryResponse> nearby(
            @RequestParam @NotNull @Min(-90) @Max(90) Double lat,
            @RequestParam @NotNull @Min(-180) @Max(180) Double lng,
            @RequestParam(defaultValue = "5") @DecimalMin("0.1") @DecimalMax("100") Double radiusKm,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer limit
    ) {
        return placeService.findNearby(lat, lng, radiusKm, categoryId, limit);
    }

    @GetMapping("/{id}")
    public PlaceResponse getById(@PathVariable UUID id) {
        return placeService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaceResponse create(@Valid @RequestBody PlaceRequest request) {
        return placeService.create(request);
    }

    @PutMapping("/{id}")
    public PlaceResponse update(@PathVariable UUID id, @Valid @RequestBody PlaceRequest request) {
        return placeService.update(id, request);
    }
}
