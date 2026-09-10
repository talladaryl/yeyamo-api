package com.yeyamo_mobile.api.partner_service.interfaces.rest;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;

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
    private final JdbcTemplate jdbc;

    public PartnerInternalController(
            SpringPartnerRepository partnerRepository,
            Profiles profilesRepository,
            JdbcTemplate jdbc
    ) {
        this.partnerRepository = partnerRepository;
        this.profilesRepository = profilesRepository;
        this.jdbc = jdbc;
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

    @GetMapping("/{partnerId}/users/{userId}/artwork-management")
    public PartnerAuthorizationResponse canManageArtwork(@PathVariable UUID partnerId, @PathVariable String userId) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .filter(value -> value.getStatus() == PartnerStatus.APPROVED)
                .orElseThrow(() -> new PartnerException("PARTNER_NOT_ACTIVE", "Partenaire inactif", HttpStatus.NOT_FOUND));
        boolean owner = userId.equals(partner.getOwnerUserId());
        Boolean member = jdbc.queryForObject("select exists(select 1 from partner_memberships where partner_id=? and user_id=? and status='ACTIVE' and revoked_at is null)", Boolean.class, partnerId, userId);
        if (!owner && !Boolean.TRUE.equals(member)) {
            throw new PartnerException("PARTNER_ACCESS_DENIED", "Accès partenaire refusé", HttpStatus.FORBIDDEN);
        }
        return new PartnerAuthorizationResponse(partnerId, true);
    }

    public record PartnerAuthorizationResponse(UUID partnerId, boolean allowed) {}

    @GetMapping("/users/{userId}/artwork-management")
    public List<UUID> artworkPartners(@PathVariable String userId) {
        return jdbc.queryForList("select distinct p.id from partners p left join partner_memberships m on m.partner_id=p.id and m.user_id=? and m.status='ACTIVE' and m.revoked_at is null where p.status='APPROVED' and (p.owner_user_id=? or m.user_id is not null)", UUID.class, userId, userId);
    }
}
