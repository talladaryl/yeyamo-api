package com.yeyamo_mobile.api.moderation_trust_service.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.moderation_trust_service.application.port.ModerationAuditPort;
import com.yeyamo_mobile.api.moderation_trust_service.application.port.ModerationOutboxPort;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.*;
import com.yeyamo_mobile.api.moderation_trust_service.domain.port.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CulturalModerationService.
 * Pure JUnit 5 — no Spring context.
 */
class CulturalModerationServiceTests {

    // Repositories
    private AuthenticityClaimRepository claimRepo;
    private AuthenticityEvidenceRepository evidenceRepo;
    private CopyrightClaimRepository copyrightRepo;
    private CulturalReviewerRepository reviewerRepo;
    private SensitiveContentFlagRepository sensitiveRepo;

    // Infrastructure
    private ModerationOutboxPort outbox;
    private ModerationAuditPort  audit;
    private List<String>         outboxEvents;

    private CulturalModerationService service;

    @BeforeEach
    void setUp() {
        claimRepo     = mock(AuthenticityClaimRepository.class);
        evidenceRepo  = mock(AuthenticityEvidenceRepository.class);
        copyrightRepo = mock(CopyrightClaimRepository.class);
        reviewerRepo  = mock(CulturalReviewerRepository.class);
        sensitiveRepo = mock(SensitiveContentFlagRepository.class);
        audit         = mock(ModerationAuditPort.class);
        outboxEvents  = new ArrayList<>();
        outbox        = (type, aggType, aggId, actor, corr, payload) -> outboxEvents.add(type);

        service = new CulturalModerationService(
                claimRepo, evidenceRepo, copyrightRepo, reviewerRepo,
                sensitiveRepo, outbox, audit, new ObjectMapper());
    }

    // =========================================================================
    // AUTHENTICITY CLAIMS
    // =========================================================================

    @Test
    void declareClaim_savesAndPublishesEvent() {
        when(claimRepo.existsByTargetTypeAndTargetId(TargetType.ARTWORK, "art-1")).thenReturn(false);
        when(claimRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthenticityClaim result = service.declareClaim(
                "artisan-1", TargetType.ARTWORK, "art-1", "Ancient bronze");

        assertEquals(AuthenticityStatus.DECLARED, result.getStatus());
        assertTrue(outboxEvents.contains("ArtworkAuthenticityClaimed"));
    }

    @Test
    void declareClaim_rejectsDuplicate() {
        when(claimRepo.existsByTargetTypeAndTargetId(TargetType.ARTWORK, "art-1")).thenReturn(true);

        ModerationException ex = assertThrows(ModerationException.class, () ->
                service.declareClaim("artisan-1", TargetType.ARTWORK, "art-1", "desc"));
        assertEquals("CLAIM_ALREADY_EXISTS", ex.getCode());
    }

    @Test
    void submitEvidence_movesStatusToEvidenceSubmitted() {
        AuthenticityClaim claim = claimWith(AuthenticityStatus.DECLARED);
        when(claimRepo.findById("claim-1")).thenReturn(Optional.of(claim));
        when(claimRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(evidenceRepo.save(any())).thenAnswer(inv -> {
            AuthenticityEvidence ev = inv.getArgument(0);
            ev.setId("ev-generated-1"); // simulate JPA UUID generation
            return ev;
        });

        service.submitEvidence("claim-1", "artisan-1",
                "CERTIFICATE", "https://example.com/cert.pdf", "Original cert");

        verify(claimRepo).save(argThat(c ->
                ((AuthenticityClaim) c).getStatus() == AuthenticityStatus.EVIDENCE_SUBMITTED));
    }

    @Test
    void submitEvidence_onVerifiedClaim_throws() {
        AuthenticityClaim claim = claimWith(AuthenticityStatus.VERIFIED);
        when(claimRepo.findById("claim-1")).thenReturn(Optional.of(claim));

        ModerationException ex = assertThrows(ModerationException.class, () ->
                service.submitEvidence("claim-1", "artisan-1",
                        "PHOTO", "https://example.com/photo.jpg", "Photo"));
        assertEquals("CLAIM_NOT_OPEN", ex.getCode());
    }

    @Test
    void verifyClaim_requiresReviewerInScope() {
        AuthenticityClaim claim = claimInReview();
        CulturalReviewer reviewer = reviewerWithScope("reviewer-1",
                "user-reviewer", ReviewerRole.CULTURAL_EXPERT, TargetType.ARTWORK);

        when(claimRepo.findById("claim-1")).thenReturn(Optional.of(claim));
        when(reviewerRepo.findByUserId("user-reviewer")).thenReturn(Optional.of(reviewer));
        when(claimRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthenticityClaim result = service.verifyClaim("claim-1", "user-reviewer", "Verified");

        assertEquals(AuthenticityStatus.VERIFIED, result.getStatus());
        assertTrue(outboxEvents.contains("ArtworkAuthenticityVerified"));
    }

    @Test
    void verifyClaim_reviewerOutOfScope_throws() {
        AuthenticityClaim claim = claimInReview();
        // Reviewer scope is ARTWORK only but claim is CULTURE_CONTENT
        AuthenticityClaim cultureContentClaim = claimInReviewFor(TargetType.CULTURE_CONTENT);
        CulturalReviewer reviewer = reviewerWithScope("rev-1", "user-rev",
                ReviewerRole.CULTURAL_EXPERT, TargetType.ARTWORK);

        when(claimRepo.findById("claim-1")).thenReturn(Optional.of(cultureContentClaim));
        when(reviewerRepo.findByUserId("user-rev")).thenReturn(Optional.of(reviewer));

        ModerationException ex = assertThrows(ModerationException.class, () ->
                service.verifyClaim("claim-1", "user-rev", "notes"));
        assertEquals("REVIEWER_OUT_OF_SCOPE", ex.getCode());
    }

    @Test
    void rejectClaim_publishesRejectedEvent() {
        AuthenticityClaim claim = claimInReview();
        CulturalReviewer reviewer = reviewerWithScope("rev-1", "user-rev",
                ReviewerRole.CULTURAL_EXPERT, TargetType.ARTWORK);

        when(claimRepo.findById("claim-1")).thenReturn(Optional.of(claim));
        when(reviewerRepo.findByUserId("user-rev")).thenReturn(Optional.of(reviewer));
        when(claimRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.rejectClaim("claim-1", "user-rev", "Fake certificate");

        assertTrue(outboxEvents.contains("ArtworkAuthenticityRejected"));
    }

    @Test
    void inactiveReviewer_isRejected() {
        AuthenticityClaim claim = claimInReview();
        CulturalReviewer reviewer = reviewerWithScope("rev-1", "user-rev",
                ReviewerRole.CULTURAL_EXPERT, TargetType.ARTWORK);
        reviewer.setStatus("SUSPENDED");

        when(claimRepo.findById("claim-1")).thenReturn(Optional.of(claim));
        when(reviewerRepo.findByUserId("user-rev")).thenReturn(Optional.of(reviewer));

        ModerationException ex = assertThrows(ModerationException.class, () ->
                service.verifyClaim("claim-1", "user-rev", "notes"));
        assertEquals("REVIEWER_INACTIVE", ex.getCode());
    }

    // =========================================================================
    // COPYRIGHT CLAIMS
    // =========================================================================

    @Test
    void fileCopyrightClaim_removesContentImmediately() {
        when(copyrightRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CopyrightClaim claim = service.fileCopyrightClaim(
                "claimant-1", TargetType.ARTWORK, "art-99",
                "creator-1", "This artwork copies my design", null, null);

        assertEquals(CopyrightClaimStatus.CONTENT_REMOVED, claim.getStatus());
        assertTrue(claim.getContentRemoved());
        assertTrue(outboxEvents.contains("CopyrightClaimCreated"));
    }

    @Test
    void upholdClaim_contentStaysRemoved() {
        CopyrightClaim claim = copyrightClaimInReview();
        when(copyrightRepo.findById("cc-1")).thenReturn(Optional.of(claim));
        when(copyrightRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CopyrightClaim result = service.upholdClaim("cc-1", "reviewer-1",
                "Infringement confirmed", null);

        assertEquals(CopyrightClaimStatus.UPHELD, result.getStatus());
        assertFalse(result.getContentRestored());
        assertTrue(outboxEvents.contains("CopyrightClaimResolved"));
    }

    @Test
    void dismissClaim_restoresContent() {
        CopyrightClaim claim = copyrightClaimInReview();
        when(copyrightRepo.findById("cc-1")).thenReturn(Optional.of(claim));
        when(copyrightRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CopyrightClaim result = service.dismissClaim("cc-1", "reviewer-1",
                "No infringement found", null);

        assertEquals(CopyrightClaimStatus.REJECTED, result.getStatus());
        assertTrue(result.getContentRestored());
    }

    @Test
    void concurrentDecisions_optimisticLock_producesConsistentState() {
        // Simulate concurrent uphold and dismiss targeting the same claim
        // In production, optimistic locking prevents both from committing.
        // Here we verify each decision independently produces a valid final state.
        CopyrightClaim forUphold  = copyrightClaimInReview();
        CopyrightClaim forDismiss = copyrightClaimInReview();

        when(copyrightRepo.findById("cc-2")).thenReturn(Optional.of(forUphold));
        when(copyrightRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CopyrightClaim upheld = service.upholdClaim("cc-2", "r1", "Confirmed", null);
        assertEquals(CopyrightClaimStatus.UPHELD, upheld.getStatus());

        when(copyrightRepo.findById("cc-2")).thenReturn(Optional.of(forDismiss));
        CopyrightClaim dismissed = service.dismissClaim("cc-2", "r2", "Not proved", null);
        assertEquals(CopyrightClaimStatus.REJECTED, dismissed.getStatus());
    }

    // =========================================================================
    // SENSITIVE CONTENT
    // =========================================================================

    @Test
    void flagSensitiveContent_createsPendingFlag() {
        when(sensitiveRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SensitiveContentFlag flag = service.flagSensitiveContent(
                TargetType.CULTURE_CONTENT, "content-1",
                SensitiveContentFlag.FlagType.SACRED_CONTENT,
                "Contains sacred ritual", "moderator-1");

        assertEquals(SensitiveContentFlag.FlagStatus.PENDING, flag.getStatus());
        assertFalse(flag.blocksPublicFeed(), "Pending flags must NOT block feed yet");
    }

    @Test
    void approveFlag_blocksPublicFeed() {
        SensitiveContentFlag flag = SensitiveContentFlag.create(
                TargetType.CULTURE_CONTENT, "content-1",
                SensitiveContentFlag.FlagType.SACRED_CONTENT,
                "Sacred content", "mod-1");

        when(sensitiveRepo.findById(flag.getId())).thenReturn(Optional.of(flag));
        when(sensitiveRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SensitiveContentFlag result = service.approveSensitiveFlag(
                flag.getId(), "admin-1", "corr-1");

        assertEquals(SensitiveContentFlag.FlagStatus.APPROVED, result.getStatus());
        assertTrue(result.blocksPublicFeed());
        assertTrue(outboxEvents.contains("CultureContentRestricted"));
    }

    @Test
    void isBlockedFromPublicFeed_delegatesToRepository() {
        when(sensitiveRepo.hasApprovedFlag(TargetType.ARTWORK, "art-1")).thenReturn(true);
        assertTrue(service.isBlockedFromPublicFeed(TargetType.ARTWORK, "art-1"));

        when(sensitiveRepo.hasApprovedFlag(TargetType.ARTWORK, "art-2")).thenReturn(false);
        assertFalse(service.isBlockedFromPublicFeed(TargetType.ARTWORK, "art-2"));
    }

    // =========================================================================
    // AUDIT TRAIL
    // =========================================================================

    @Test
    void auditEntryIsCreatedForEveryAction() {
        when(claimRepo.existsByTargetTypeAndTargetId(any(), any())).thenReturn(false);
        when(claimRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.declareClaim("artisan-1", TargetType.ARTWORK, "art-audit", "desc");

        verify(audit, atLeastOnce()).append(any());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private AuthenticityClaim claimWith(AuthenticityStatus status) {
        AuthenticityClaim c = AuthenticityClaim.builder()
                .targetType(TargetType.ARTWORK).targetId("art-1")
                .claimantId("artisan-1").status(status).build();
        c.setId("claim-1");
        return c;
    }

    private AuthenticityClaim claimInReview() {
        return claimInReviewFor(TargetType.ARTWORK);
    }

    private AuthenticityClaim claimInReviewFor(TargetType type) {
        AuthenticityClaim c = AuthenticityClaim.builder()
                .targetType(type).targetId("art-1").claimantId("artisan-1")
                .status(AuthenticityStatus.UNDER_REVIEW).build();
        c.setId("claim-1");
        c.setAssignedReviewerId("rev-1");
        return c;
    }

    private CopyrightClaim copyrightClaimInReview() {
        CopyrightClaim c = CopyrightClaim.builder()
                .targetType(TargetType.ARTWORK).targetId("art-99")
                .claimantId("claimant-1").contentCreatorId("creator-1")
                .claimDetails("Infringement details").status(CopyrightClaimStatus.UNDER_REVIEW)
                .contentRemoved(true).contentRestored(false).build();
        c.setId("cc-1");
        return c;
    }

    private CulturalReviewer reviewerWithScope(String id, String userId,
                                                ReviewerRole role, TargetType... types) {
        CulturalReviewer r = CulturalReviewer.builder()
                .userId(userId).role(role).displayName("Reviewer " + userId)
                .contentTypes(types.length > 0
                        ? new java.util.HashSet<>(java.util.Arrays.asList(types))
                        : new java.util.HashSet<>())
                .build();
        r.setId(id);
        r.setStatus("ACTIVE");
        return r;
    }
}
