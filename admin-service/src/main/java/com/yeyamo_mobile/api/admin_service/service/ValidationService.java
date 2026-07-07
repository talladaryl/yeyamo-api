package com.yeyamo_mobile.api.admin_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.admin_service.dto.PartnerReviewRequest;
import com.yeyamo_mobile.api.admin_service.dto.PartnerValidationRequest;
import com.yeyamo_mobile.api.admin_service.dto.PlaceReviewRequest;
import com.yeyamo_mobile.api.admin_service.dto.PlaceValidationRequest;
import com.yeyamo_mobile.api.admin_service.enums.ValidationStatus;
import com.yeyamo_mobile.api.admin_service.exception.ApiException;
import com.yeyamo_mobile.api.admin_service.models.PartnerValidation;
import com.yeyamo_mobile.api.admin_service.models.PlaceValidation;
import com.yeyamo_mobile.api.admin_service.repository.PartnerValidationRepository;
import com.yeyamo_mobile.api.admin_service.repository.PlaceValidationRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ValidationService {

    private final PartnerValidationRepository partnerRepository;
    private final PlaceValidationRepository placeRepository;
    private final AuditService auditService;

    public ValidationService(PartnerValidationRepository partnerRepository, PlaceValidationRepository placeRepository, AuditService auditService) {
        this.partnerRepository = partnerRepository;
        this.placeRepository = placeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PartnerValidation> listPartners(ValidationStatus status) {
        return status == null ? partnerRepository.findAllByOrderByCreatedAtDesc() : partnerRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public PartnerValidation createPartnerValidation(PartnerValidationRequest request) {
        PartnerValidation validation = new PartnerValidation();
        validation.setPartnerId(request.partnerId());
        validation.setRequesterId(request.requesterId());
        validation.setKycDocumentUrls(request.safeKycDocumentUrls());
        validation.setKycDocumentTypes(request.safeKycDocumentTypes());
        validation.setReviewComment(request.reviewComment());
        validation.setRiskScore(request.riskScore());
        return partnerRepository.save(validation);
    }

    @Transactional
    public PartnerValidation reviewPartner(UUID id, PartnerReviewRequest request, HttpServletRequest httpRequest) {
        PartnerValidation validation = partnerRepository.findById(id)
                .orElseThrow(() -> new ApiException("PARTNER_VALIDATION_NOT_FOUND", "Validation partenaire introuvable", HttpStatus.NOT_FOUND));
        validation.setStatus(request.status());
        validation.setReviewComment(request.reviewComment());
        validation.setRiskScore(request.riskScore());
        if (request.status() == ValidationStatus.APPROVED || request.status() == ValidationStatus.REJECTED) {
            validation.setValidatedBy(request.validatedBy() == null ? auditService.currentAdminId() : request.validatedBy());
            validation.setValidatedAt(LocalDateTime.now());
        }
        PartnerValidation saved = partnerRepository.save(validation);
        auditService.record("VALIDATE_PARTNER", "PARTNER", saved.getPartnerId(), Map.of("status", saved.getStatus().name()), httpRequest);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PlaceValidation> listPlaces(ValidationStatus status) {
        return status == null ? placeRepository.findAllByOrderByCreatedAtDesc() : placeRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public PlaceValidation createPlaceValidation(PlaceValidationRequest request) {
        PlaceValidation validation = new PlaceValidation();
        validation.setPlaceId(request.placeId());
        validation.setSubmittedBy(request.submittedBy());
        validation.setReviewComment(request.reviewComment());
        validation.setChangesRequested(request.safeChangesRequested());
        return placeRepository.save(validation);
    }

    @Transactional
    public PlaceValidation reviewPlace(UUID id, PlaceReviewRequest request, HttpServletRequest httpRequest) {
        PlaceValidation validation = placeRepository.findById(id)
                .orElseThrow(() -> new ApiException("PLACE_VALIDATION_NOT_FOUND", "Validation lieu introuvable", HttpStatus.NOT_FOUND));
        validation.setStatus(request.status());
        validation.setReviewedBy(request.reviewedBy() == null ? auditService.currentAdminId() : request.reviewedBy());
        validation.setReviewComment(request.reviewComment());
        validation.setChangesRequested(request.safeChangesRequested());
        if (request.status() == ValidationStatus.APPROVED) {
            validation.setApprovedAt(LocalDateTime.now());
        }
        PlaceValidation saved = placeRepository.save(validation);
        auditService.record("VALIDATE_PLACE", "PLACE", saved.getPlaceId(), Map.of("status", saved.getStatus().name()), httpRequest);
        return saved;
    }
}
