package com.yeyamo_mobile.api.place_service.controller;

import java.util.UUID;

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

import com.yeyamo_mobile.api.place_service.dto.DistrictRequest;
import com.yeyamo_mobile.api.place_service.dto.DistrictResponse;
import com.yeyamo_mobile.api.place_service.service.DistrictService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/districts")
public class DistrictController {

    private final DistrictService districtService;

    public DistrictController(DistrictService districtService) {
        this.districtService = districtService;
    }

    @GetMapping("/city/{cityId}")
    public List<DistrictResponse> listByCity(@PathVariable UUID cityId) {
        return districtService.listByCity(cityId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DistrictResponse create(@Valid @RequestBody DistrictRequest request) {
        return districtService.create(request);
    }

    @PutMapping("/{id}")
    public DistrictResponse update(@PathVariable Long id, @Valid @RequestBody DistrictRequest request) {
        return districtService.update(id, request);
    }
    @PatchMapping("/{id}/status") @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") public DistrictResponse status(@PathVariable Long id,@RequestBody ReferenceStatusRequest request){return districtService.setActive(id,request.active());}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('SUPER_ADMIN')") public void delete(@PathVariable Long id){districtService.delete(id);}
}
