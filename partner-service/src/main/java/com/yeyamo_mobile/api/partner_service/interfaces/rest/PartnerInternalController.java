package com.yeyamo_mobile.api.partner_service.interfaces.rest;

import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.partner_service.application.PartnerException;
import com.yeyamo_mobile.api.partner_service.domain.model.PartnerStatus;
import com.yeyamo_mobile.api.partner_service.infrastructure.persistence.ArtisanProfileEntity;
import com.yeyamo_mobile.api.partner_service.infrastructure.persistence.ArtisanRepositories.Profiles;
import com.yeyamo_mobile.api.partner_service.infrastructure.persistence.PartnerEntity;
import com.yeyamo_mobile.api.partner_service.infrastructure.persistence.SpringPartnerRepository;

@RestController
@RequestMapping("/internal/partners")
public class PartnerInternalController {

    private final SpringPartnerRepository partnerRepository;
    private final Profiles profilesRepository;

    public PartnerInternalController(
            SpringPartnerRepository partnerRepository,
            Profiles profilesRepository
    ) {
        this.partnerRepository = partnerRepository;
        this.profilesRepository = profilesRepository;
    }

    @GetMapping("/{partnerId}/user-id")
    public PartnerUserIdResponse getUserId(@PathVariable UUID partnerId) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new PartnerException("PARTNER_NOT_FOUND", "Partenaire introuvable", HttpStatus.NOT_FOUND));

        boolean isApprovedPartner = partner.getStatus() == PartnerStatus.APPROVED || partner.getVerifiedAt() != null;
        boolean isVerifiedArtisan = profilesRepository.findById(partnerId)
                .map(profile -> profile.getVerificationStatus() == ArtisanProfileEntity.VerificationStatus.VERIFIED)
                .orElse(false);

        if (!isApprovedPartner && !isVerifiedArtisan) {
            throw new PartnerException("PARTNER_NOT_FOUND", "Partenaire introuvable", HttpStatus.NOT_FOUND);
        }

        String userId = partner.getOwnerUserId();
        if (userId == null || userId.isBlank()) {
            throw new PartnerException("PARTNER_USER_NOT_FOUND", "Identifiant utilisateur introuvable pour ce partenaire", HttpStatus.NOT_FOUND);
        }

        return new PartnerUserIdResponse(userId);
    }

    public record PartnerUserIdResponse(String userId) {}
}
