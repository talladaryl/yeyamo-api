package com.yeyamo_mobile.api.partner_service.interfaces.rest;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.yeyamo.security.hardening.InternalServiceTokenFilter;
import com.yeyamo_mobile.api.partner_service.application.PartnerException;
import com.yeyamo_mobile.api.partner_service.domain.model.PartnerStatus;
import com.yeyamo_mobile.api.partner_service.infrastructure.persistence.ArtisanProfileEntity;
import com.yeyamo_mobile.api.partner_service.infrastructure.persistence.ArtisanRepositories.Profiles;
import com.yeyamo_mobile.api.partner_service.infrastructure.persistence.PartnerEntity;
import com.yeyamo_mobile.api.partner_service.infrastructure.persistence.SpringPartnerRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PartnerInternalControllerTest {

    private SpringPartnerRepository partnerRepository;
    private Profiles profilesRepository;
    private PartnerInternalController controller;

    @BeforeEach
    void setUp() {
        partnerRepository = mock(SpringPartnerRepository.class);
        profilesRepository = mock(Profiles.class);
        controller = new PartnerInternalController(partnerRepository, profilesRepository);
    }

    @Test
    @DisplayName("Devrait retourner l'ownerUserId quand le partenaire a le statut APPROVED")
    void shouldReturnUserIdWhenPartnerIsApproved() {
        UUID partnerId = UUID.randomUUID();
        PartnerEntity partner = new PartnerEntity();
        partner.setId(partnerId);
        partner.setStatus(PartnerStatus.APPROVED);
        partner.setOwnerUserId("user-owner-123");

        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));

        var response = controller.getUserId(partnerId);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo("user-owner-123");
    }

    @Test
    @DisplayName("Devrait retourner l'ownerUserId quand le partenaire est DRAFT mais son profil artisan est VERIFIED")
    void shouldReturnUserIdWhenArtisanProfileIsVerified() {
        UUID partnerId = UUID.randomUUID();
        PartnerEntity partner = new PartnerEntity();
        partner.setId(partnerId);
        partner.setStatus(PartnerStatus.DRAFT);
        partner.setOwnerUserId("artisan-user-456");

        ArtisanProfileEntity profile = mock(ArtisanProfileEntity.class);
        when(profile.getVerificationStatus()).thenReturn(ArtisanProfileEntity.VerificationStatus.VERIFIED);

        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));
        when(profilesRepository.findById(partnerId)).thenReturn(Optional.of(profile));

        var response = controller.getUserId(partnerId);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo("artisan-user-456");
    }

    @Test
    @DisplayName("Devrait lancer PARTNER_NOT_FOUND (404) quand le partenaire n'existe pas")
    void shouldThrowNotFoundWhenPartnerDoesNotExist() {
        UUID partnerId = UUID.randomUUID();
        when(partnerRepository.findById(partnerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getUserId(partnerId))
                .isInstanceOf(PartnerException.class)
                .satisfies(ex -> {
                    PartnerException pe = (PartnerException) ex;
                    assertThat(pe.getCode()).isEqualTo("PARTNER_NOT_FOUND");
                    assertThat(pe.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("Devrait obfusquer le partenaire non vérifié en PARTNER_NOT_FOUND (404)")
    void shouldObfuscateUnverifiedPartnerAsNotFound() {
        UUID partnerId = UUID.randomUUID();
        PartnerEntity partner = new PartnerEntity();
        partner.setId(partnerId);
        partner.setStatus(PartnerStatus.DRAFT);
        partner.setOwnerUserId("artisan-user-draft");

        ArtisanProfileEntity profile = mock(ArtisanProfileEntity.class);
        when(profile.getVerificationStatus()).thenReturn(ArtisanProfileEntity.VerificationStatus.PENDING);

        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));
        when(profilesRepository.findById(partnerId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> controller.getUserId(partnerId))
                .isInstanceOf(PartnerException.class)
                .satisfies(ex -> {
                    PartnerException pe = (PartnerException) ex;
                    assertThat(pe.getCode()).isEqualTo("PARTNER_NOT_FOUND");
                    assertThat(pe.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("Devrait lancer PARTNER_USER_NOT_FOUND (404) quand ownerUserId est null ou vide")
    void shouldThrowUserNotFoundWhenOwnerUserIdIsBlank() {
        UUID partnerId = UUID.randomUUID();
        PartnerEntity partner = new PartnerEntity();
        partner.setId(partnerId);
        partner.setStatus(PartnerStatus.APPROVED);
        partner.setOwnerUserId("   ");

        when(partnerRepository.findById(partnerId)).thenReturn(Optional.of(partner));

        assertThatThrownBy(() -> controller.getUserId(partnerId))
                .isInstanceOf(PartnerException.class)
                .satisfies(ex -> {
                    PartnerException pe = (PartnerException) ex;
                    assertThat(pe.getCode()).isEqualTo("PARTNER_USER_NOT_FOUND");
                    assertThat(pe.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("InternalServiceTokenFilter doit bloquer avec 401 si le token est manquant ou invalide")
    void internalFilterShouldRejectMissingOrInvalidToken() throws Exception {
        InternalServiceTokenFilter filter = new InternalServiceTokenFilter("test-secret-token");

        // 1. Missing token
        MockHttpServletRequest reqMissing = new MockHttpServletRequest("GET", "/internal/partners/123/user-id");
        MockHttpServletResponse resMissing = new MockHttpServletResponse();
        MockFilterChain chainMissing = new MockFilterChain();
        filter.doFilter(reqMissing, resMissing, chainMissing);
        assertThat(resMissing.getStatus()).isEqualTo(401);
        assertThat(resMissing.getContentAsString()).contains("INTERNAL_UNAUTHORIZED");

        // 2. Invalid token
        MockHttpServletRequest reqInvalid = new MockHttpServletRequest("GET", "/internal/partners/123/user-id");
        reqInvalid.addHeader("X-Internal-Token", "wrong-token");
        MockHttpServletResponse resInvalid = new MockHttpServletResponse();
        MockFilterChain chainInvalid = new MockFilterChain();
        filter.doFilter(reqInvalid, resInvalid, chainInvalid);
        assertThat(resInvalid.getStatus()).isEqualTo(401);

        // 3. Valid token
        MockHttpServletRequest reqValid = new MockHttpServletRequest("GET", "/internal/partners/123/user-id");
        reqValid.addHeader("X-Internal-Token", "test-secret-token");
        MockHttpServletResponse resValid = new MockHttpServletResponse();
        MockFilterChain chainValid = new MockFilterChain();
        filter.doFilter(reqValid, resValid, chainValid);
        assertThat(resValid.getStatus()).isEqualTo(200);
    }
}
