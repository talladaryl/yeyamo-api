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
import com.yeyamo_mobile.api.admin_service.event.AdminEventOutbox;
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
    private final AdminEventOutbox eventOutbox;

    public ValidationService(PartnerValidationRepository partnerRepository, PlaceValidationRepository placeRepository, AuditService auditService, AdminEventOutbox eventOutbox) {
        this.partnerRepository = partnerRepository;
        this.placeRepository = placeRepository;
        this.auditService = auditService;
        this.eventOutbox = eventOutbox;
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
    public PartnerValidation createPartnerValidationFromEvent(UUID partnerId, List<String> documentKeys,
            List<String> documentTypes) {
        PartnerValidation validation = new PartnerValidation();
        validation.setPartnerId(partnerId);
        validation.setKycDocumentUrls(documentKeys == null ? List.of() : documentKeys);
        validation.setKycDocumentTypes(documentTypes == null ? List.of() : documentTypes);
        return partnerRepository.save(validation);
    }

    @Transactional
    public PartnerValidation reviewPartner(UUID id, PartnerReviewRequest request, HttpServletRequest httpRequest) {
        PartnerValidation validation = partnerRepository.findById(id)
                .orElseThrow(() -> new ApiException("PARTNER_VALIDATION_NOT_FOUND", "Validation partenaire introuvable", HttpStatus.NOT_FOUND));
        validation.setStatus(request.decision());
        validation.setReviewComment(request.reason() == null ? request.comment() : request.reason());
        validation.setRiskScore(request.riskScore());
        if (request.decision() == ValidationStatus.APPROVED || request.decision() == ValidationStatus.REJECTED
                || request.decision() == ValidationStatus.REQUIRES_CHANGES || request.decision() == ValidationStatus.CORRECTIONS_REQUIRED
                || request.decision() == ValidationStatus.NEEDS_INFO) {
            validation.setValidatedBy(auditService.currentAdminId());
            validation.setValidatedAt(LocalDateTime.now());
        }
        PartnerValidation saved = partnerRepository.save(validation);
        auditService.record("VALIDATE_PARTNER", "PARTNER", saved.getPartnerId(), Map.of("status", saved.getStatus().name()), httpRequest);
        String eventType = partnerEventType(saved.getStatus());
        if (eventType != null) {
            eventOutbox.partnerDecision(eventType, saved.getPartnerId(), saved.getId(), saved.getReviewComment(),
                    saved.getRiskScore(), saved.getValidatedBy(), httpRequest.getHeader("X-Correlation-ID"));
        }
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

    private String partnerEventType(ValidationStatus status) {
        return switch (status) {
            case UNDER_REVIEW -> "partner.under_review";
            case APPROVED -> "partner.approved";
            case REJECTED -> "partner.rejected";
            case NEEDS_INFO -> "partner.needs_info";
            case REQUIRES_CHANGES -> "partner.requires_changes";
            case CORRECTIONS_REQUIRED -> "partner.requires_changes";
            case PENDING -> null;
        };
    }
}
