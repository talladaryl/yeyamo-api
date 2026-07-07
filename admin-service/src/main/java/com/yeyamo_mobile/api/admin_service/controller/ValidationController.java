package com.yeyamo_mobile.api.admin_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.admin_service.dto.PartnerReviewRequest;
import com.yeyamo_mobile.api.admin_service.dto.PartnerValidationRequest;
import com.yeyamo_mobile.api.admin_service.dto.PlaceReviewRequest;
import com.yeyamo_mobile.api.admin_service.dto.PlaceValidationRequest;
import com.yeyamo_mobile.api.admin_service.enums.ValidationStatus;
import com.yeyamo_mobile.api.admin_service.models.PartnerValidation;
import com.yeyamo_mobile.api.admin_service.models.PlaceValidation;
import com.yeyamo_mobile.api.admin_service.service.ValidationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/validations")
@Validated
public class ValidationController {

    private final ValidationService service;

    public ValidationController(ValidationService service) {
        this.service = service;
    }

    @GetMapping("/partners")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public List<PartnerValidation> listPartners(@RequestParam(required = false) ValidationStatus status) {
        return service.listPartners(status);
    }

    @PostMapping("/partners")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public PartnerValidation createPartner(@Valid @RequestBody PartnerValidationRequest request) {
        return service.createPartnerValidation(request);
    }

    @PatchMapping("/partners/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public PartnerValidation reviewPartner(@PathVariable UUID id, @Valid @RequestBody PartnerReviewRequest request, HttpServletRequest httpRequest) {
        return service.reviewPartner(id, request, httpRequest);
    }

    @GetMapping("/places")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')")
    public List<PlaceValidation> listPlaces(@RequestParam(required = false) ValidationStatus status) {
        return service.listPlaces(status);
    }

    @PostMapping("/places")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')")
    public PlaceValidation createPlace(@Valid @RequestBody PlaceValidationRequest request) {
        return service.createPlaceValidation(request);
    }

    @PatchMapping("/places/{id}/review")
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')")
    public PlaceValidation reviewPlace(@PathVariable UUID id, @Valid @RequestBody PlaceReviewRequest request, HttpServletRequest httpRequest) {
        return service.reviewPlace(id, request, httpRequest);
    }
}
