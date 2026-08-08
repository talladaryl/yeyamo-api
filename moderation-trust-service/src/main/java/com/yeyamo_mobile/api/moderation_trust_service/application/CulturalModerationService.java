package com.yeyamo_mobile.api.moderation_trust_service.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.moderation_trust_service.application.port.ModerationAuditPort;
import com.yeyamo_mobile.api.moderation_trust_service.application.port.ModerationOutboxPort;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.*;
import com.yeyamo_mobile.api.moderation_trust_service.domain.port.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles cultural moderation: authenticity claims, copyright claims, sensitive content.
 * All mutations are wrapped in transactions + Transactional Outbox + immutable audit.
 */
@Service
public class CulturalModerationService {

    private static final Logger log = LoggerFactory.getLogger(CulturalModerationService.class);

    private final AuthenticityClaimRepository claimRepo;
    private final AuthenticityEvidenceRepository evidenceRepo;
    private final CopyrightClaimRepository copyrightRepo;
    private final CulturalReviewerRepository reviewerRepo;
    private final SensitiveContentFlagRepository sensitiveRepo;
    private final ModerationOutboxPort outbox;
    private final ModerationAuditPort audit;
    private final ObjectMapper mapper;

    public CulturalModerationService(
            AuthenticityClaimRepository claimRepo,
            AuthenticityEvidenceRepository evidenceRepo,
            CopyrightClaimRepository copyrightRepo,
            CulturalReviewerRepository reviewerRepo,
            SensitiveContentFlagRepository sensitiveRepo,
            ModerationOutboxPort outbox,
            ModerationAuditPort audit,
            ObjectMapper mapper) {
        this.claimRepo    = claimRepo;
        this.evidenceRepo = evidenceRepo;
        this.copyrightRepo = copyrightRepo;
        this.reviewerRepo = reviewerRepo;
        this.sensitiveRepo = sensitiveRepo;
        this.outbox = outbox;
        this.audit  = audit;
        this.mapper = mapper;
    }

    // =========================================================================
    // AUTHENTICITY CLAIMS
    // =========================================================================

    @Transactional
    public AuthenticityClaim declareClaim(String claimantId, TargetType targetType,
                                          String targetId, String description) {
        if (claimRepo.existsByTargetTypeAndTargetId(targetType, targetId))
            throw new ModerationException("CLAIM_ALREADY_EXISTS",
                    "A claim already exists for this content");

        AuthenticityClaim claim = AuthenticityClaim.builder()
                .targetType(targetType).targetId(targetId).claimantId(claimantId)
                .status(AuthenticityStatus.DECLARED).claimDescription(description).build();
        claim = claimRepo.save(claim);

        record("AUTHENTICITY_CLAIM_DECLARED", claim.getId(), claimantId, null,
               Map.of("targetType", targetType, "targetId", targetId));
        outbox.append("ArtworkAuthenticityClaimed", "authenticity-claim", claim.getId(),
               claimantId, null, claimPayload(claim));
        log.info("Claim {} declared for {} {}", claim.getId(), targetType, targetId);
        return claim;
    }

    @Transactional
    public AuthenticityEvidence submitEvidence(String claimId, String uploadedBy,
                                               String evidenceType, String mediaUrl,
                                               String description) {
        AuthenticityClaim claim = requireClaim(claimId);
        if (claim.getStatus() == AuthenticityStatus.VERIFIED
                || claim.getStatus() == AuthenticityStatus.REVOKED)
            throw new ModerationException("CLAIM_NOT_OPEN",
                    "Evidence cannot be added to a " + claim.getStatus() + " claim");

        AuthenticityEvidence ev = AuthenticityEvidence.builder()
                .claimId(claimId).evidenceType(evidenceType)
                .mediaUrl(mediaUrl).description(description).uploadedBy(uploadedBy).build();
        ev = evidenceRepo.save(ev);

        if (claim.getStatus() == AuthenticityStatus.DECLARED) {
            claim.setStatus(AuthenticityStatus.EVIDENCE_SUBMITTED);
            claimRepo.save(claim);
        }
        record("AUTHENTICITY_EVIDENCE_SUBMITTED", claimId, uploadedBy, null,
               Map.of("evidenceId", ev.getId(), "evidenceType", evidenceType));
        return ev;
    }

    @Transactional
    public void assignForReview(String claimId, String reviewerUserId, String actorId) {
        AuthenticityClaim claim    = requireClaim(claimId);
        CulturalReviewer  reviewer = requireActiveInScope(reviewerUserId, claim.getTargetType());
        claim.setAssignedReviewerId(reviewer.getId());
        claim.setStatus(AuthenticityStatus.UNDER_REVIEW);
        claimRepo.save(claim);
        record("AUTHENTICITY_CLAIM_ASSIGNED", claimId, actorId, null,
               Map.of("reviewerId", reviewer.getId()));
    }

    @Transactional
    public AuthenticityClaim verifyClaim(String claimId, String reviewerUserId, String notes) {
        AuthenticityClaim claim    = requireClaim(claimId);
        CulturalReviewer  reviewer = requireActiveInScope(reviewerUserId, claim.getTargetType());
        claim.verify(reviewer.getId());
        claim.setReviewerNotes(notes);
        claim = claimRepo.save(claim);
        record("AUTHENTICITY_CLAIM_VERIFIED", claimId, reviewerUserId, null, Map.of());
        outbox.append("ArtworkAuthenticityVerified", "authenticity-claim", claimId,
               reviewerUserId, null, claimPayload(claim));
        return claim;
    }

    @Transactional
    public AuthenticityClaim rejectClaim(String claimId, String reviewerUserId, String reason) {
        AuthenticityClaim claim    = requireClaim(claimId);
        CulturalReviewer  reviewer = requireActiveInScope(reviewerUserId, claim.getTargetType());
        claim.reject(reviewer.getId(), reason);
        claim = claimRepo.save(claim);
        record("AUTHENTICITY_CLAIM_REJECTED", claimId, reviewerUserId, null,
               Map.of("reason", reason));
        outbox.append("ArtworkAuthenticityRejected", "authenticity-claim", claimId,
               reviewerUserId, null, claimPayload(claim));
        return claim;
    }

    @Transactional
    public AuthenticityClaim revokeClaim(String claimId, String adminId, String reason) {
        AuthenticityClaim claim = requireClaim(claimId);
        claim.revoke(reason);
        claim = claimRepo.save(claim);
        record("AUTHENTICITY_CLAIM_REVOKED", claimId, adminId, null, Map.of("reason", reason));
        outbox.append("ArtworkAuthenticityRevoked", "authenticity-claim", claimId,
               adminId, null, claimPayload(claim));
        return claim;
    }

    @Transactional(readOnly = true)
    public AuthenticityClaim getClaim(String id) { return requireClaim(id); }

    @Transactional(readOnly = true)
    public List<AuthenticityEvidence> getEvidence(String claimId) {
        return evidenceRepo.findByClaimId(claimId);
    }

    // =========================================================================
    // COPYRIGHT CLAIMS
    // =========================================================================

    @Transactional
    public CopyrightClaim fileCopyrightClaim(String claimantId, TargetType targetType,
            String targetId, String contentCreatorId,
            String claimDetails, String proofUrl, String correlationId) {
        CopyrightClaim claim = CopyrightClaim.builder()
                .targetType(targetType).targetId(targetId).claimantId(claimantId)
                .contentCreatorId(contentCreatorId).claimDetails(claimDetails)
                .proofUrl(proofUrl).build();
        claim.removeContent();
        claim = copyrightRepo.save(claim);
        record("COPYRIGHT_CLAIM_FILED", claim.getId(), claimantId, correlationId,
               Map.of("targetType", targetType, "targetId", targetId));
        outbox.append("CopyrightClaimCreated", "copyright-claim", claim.getId(),
               claimantId, correlationId, copyrightPayload(claim));
        return claim;
    }

    @Transactional
    public CopyrightClaim respondToClaim(String claimId, String creatorId,
                                          String response, String correlationId) {
        CopyrightClaim claim = requireCopyrightClaim(claimId);
        claim.recordCreatorResponse(response);
        claim = copyrightRepo.save(claim);
        record("COPYRIGHT_CLAIM_RESPONSE", claimId, creatorId, correlationId, Map.of());
        return claim;
    }

    @Transactional
    public CopyrightClaim upholdClaim(String claimId, String reviewerId,
                                       String resolution, String correlationId) {
        CopyrightClaim claim = requireCopyrightClaim(claimId);
        claim.uphold(reviewerId, resolution);
        claim = copyrightRepo.save(claim);
        record("COPYRIGHT_CLAIM_UPHELD", claimId, reviewerId, correlationId,
               Map.of("resolution", resolution));
        outbox.append("CopyrightClaimResolved", "copyright-claim", claimId,
               reviewerId, correlationId, copyrightPayload(claim));
        return claim;
    }

    @Transactional
    public CopyrightClaim dismissClaim(String claimId, String reviewerId,
                                        String resolution, String correlationId) {
        CopyrightClaim claim = requireCopyrightClaim(claimId);
        claim.reject(reviewerId, resolution);
        claim = copyrightRepo.save(claim);
        record("COPYRIGHT_CLAIM_DISMISSED", claimId, reviewerId, correlationId,
               Map.of("resolution", resolution));
        outbox.append("CopyrightClaimResolved", "copyright-claim", claimId,
               reviewerId, correlationId, copyrightPayload(claim));
        return claim;
    }

    @Transactional(readOnly = true)
    public CopyrightClaim getCopyrightClaim(String id) { return requireCopyrightClaim(id); }

    @Transactional(readOnly = true)
    public List<CopyrightClaim> listCreatorCopyrightClaims(String creatorId) {
        return copyrightRepo.findByContentCreatorId(creatorId);
    }

    // =========================================================================
    // SENSITIVE CONTENT
    // =========================================================================

    @Transactional
    public SensitiveContentFlag flagSensitiveContent(TargetType targetType, String targetId,
            SensitiveContentFlag.FlagType flagType, String reason, String flaggedBy) {
        SensitiveContentFlag flag = SensitiveContentFlag.create(
                targetType, targetId, flagType, reason, flaggedBy);
        flag = sensitiveRepo.save(flag);
        record("SENSITIVE_CONTENT_FLAGGED", flag.getId(), flaggedBy, null,
               Map.of("targetType", targetType, "targetId", targetId, "flagType", flagType));
        return flag;
    }

    @Transactional
    public SensitiveContentFlag approveSensitiveFlag(String flagId, String adminId,
                                                      String correlationId) {
        SensitiveContentFlag flag = sensitiveRepo.findById(flagId)
                .orElseThrow(() -> new ModerationException("FLAG_NOT_FOUND",
                        "Sensitive flag not found: " + flagId));
        flag.approve(adminId);
        flag = sensitiveRepo.save(flag);
        record("SENSITIVE_CONTENT_FLAG_APPROVED", flagId, adminId, correlationId,
               Map.of("targetType", flag.getTargetType(), "targetId", flag.getTargetId(),
                      "flagType", flag.getFlagType()));
        outbox.append("CultureContentRestricted", "sensitive-content-flag", flagId,
               adminId, correlationId,
               Map.of("targetType", flag.getTargetType().name(), "targetId", flag.getTargetId(),
                      "flagType", flag.getFlagType().name(),
                      "blocksPublicFeed", flag.blocksPublicFeed()));
        return flag;
    }

    @Transactional(readOnly = true)
    public boolean isBlockedFromPublicFeed(TargetType targetType, String targetId) {
        return sensitiveRepo.hasApprovedFlag(targetType, targetId);
    }

    // =========================================================================
    // REVIEWER MANAGEMENT
    // =========================================================================

    @Transactional
    public CulturalReviewer registerReviewer(CulturalReviewer reviewer, String actorId) {
        reviewer = reviewerRepo.save(reviewer);
        record("CULTURAL_REVIEWER_REGISTERED", reviewer.getId(), actorId, null,
               Map.of("userId", reviewer.getUserId(), "role", reviewer.getRole()));
        return reviewer;
    }

    @Transactional(readOnly = true)
    public List<CulturalReviewer> findEligibleReviewers(TargetType contentType) {
        return reviewerRepo.findEligibleReviewers(contentType, 50);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private AuthenticityClaim requireClaim(String id) {
        return claimRepo.findById(id)
                .orElseThrow(() -> new ModerationException("CLAIM_NOT_FOUND",
                        "Authenticity claim not found: " + id));
    }

    private CopyrightClaim requireCopyrightClaim(String id) {
        return copyrightRepo.findById(id)
                .orElseThrow(() -> new ModerationException("CLAIM_NOT_FOUND",
                        "Copyright claim not found: " + id));
    }

    private CulturalReviewer requireActiveInScope(String reviewerUserId, TargetType targetType) {
        CulturalReviewer r = reviewerRepo.findByUserId(reviewerUserId)
                .orElseThrow(() -> new ModerationException("REVIEWER_NOT_FOUND",
                        "Cultural reviewer not found: " + reviewerUserId));
        if (!r.isActive())
            throw new ModerationException("REVIEWER_INACTIVE",
                    "Reviewer " + reviewerUserId + " is not active");
        if (!r.canReviewContentType(targetType))
            throw new ModerationException("REVIEWER_OUT_OF_SCOPE",
                    "Reviewer " + reviewerUserId + " is not authorized for " + targetType);
        return r;
    }

    private void record(String action, String aggregateId, String actorId,
                        String correlationId, Map<String, Object> details) {
        try {
            audit.append(AuditEntry.create(action, "CULTURAL_MODERATION",
                    aggregateId, actorId, correlationId,
                    mapper.writeValueAsString(details)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize audit entry", e);
        }
    }

    private Map<String, Object> claimPayload(AuthenticityClaim c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("claimId", c.getId()); m.put("targetType", c.getTargetType().name());
        m.put("targetId", c.getTargetId()); m.put("claimantId", c.getClaimantId());
        m.put("status", c.getStatus().name()); m.put("reviewerId", c.getAssignedReviewerId());
        return m;
    }

    private Map<String, Object> copyrightPayload(CopyrightClaim c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("claimId", c.getId()); m.put("targetType", c.getTargetType().name());
        m.put("targetId", c.getTargetId()); m.put("claimantId", c.getClaimantId());
        m.put("contentCreatorId", c.getContentCreatorId());
        m.put("status", c.getStatus().name());
        m.put("contentRemoved", c.getContentRemoved());
        m.put("contentRestored", c.getContentRestored());
        return m;
    }
}
