package com.yeyamo_mobile.api.moderation_trust_service.interfaces.rest;

import com.yeyamo_mobile.api.moderation_trust_service.application.CulturalModerationService;
import com.yeyamo_mobile.api.moderation_trust_service.domain.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for cultural content moderation:
 *   - Authenticity claims & evidence
 *   - Copyright claims
 *   - Sensitive content flags
 *   - Cultural reviewer management
 */
@RestController
@RequestMapping("/api/v1/moderation/culture")
@Tag(name = "Cultural Moderation")
@SecurityRequirement(name = "bearerAuth")
public class CulturalModerationController {

    private final CulturalModerationService service;

    public CulturalModerationController(CulturalModerationService service) {
        this.service = service;
    }

    // ---- Authenticity Claims ------------------------------------------------

    @PostMapping("/authenticity/claims")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Declare an authenticity claim for an artwork or cultural content")
    public AuthenticityClaim declareClaim(
            @Valid @RequestBody AuthenticityClaimRequest req,
            Authentication auth) {
        return service.declareClaim(auth.getName(), req.targetType(), req.targetId(),
                req.description());
    }

    @GetMapping("/authenticity/claims/{claimId}")
    @Operation(summary = "Get authenticity claim by ID")
    public AuthenticityClaim getClaim(@PathVariable String claimId) {
        return service.getClaim(claimId);
    }

    @PostMapping("/authenticity/claims/{claimId}/evidence")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit evidence for an authenticity claim")
    public AuthenticityEvidence submitEvidence(
            @PathVariable String claimId,
            @Valid @RequestBody EvidenceRequest req,
            Authentication auth) {
        return service.submitEvidence(claimId, auth.getName(),
                req.evidenceType(), req.mediaUrl(), req.description());
    }

    @GetMapping("/authenticity/claims/{claimId}/evidence")
    @Operation(summary = "List evidence for a claim")
    public List<AuthenticityEvidence> getEvidence(@PathVariable String claimId) {
        return service.getEvidence(claimId);
    }

    @PostMapping("/authenticity/claims/{claimId}/assign")
    @Operation(summary = "Assign claim to a cultural reviewer")
    public void assignForReview(
            @PathVariable String claimId,
            @Valid @RequestBody AssignReviewerRequest req,
            Authentication auth) {
        service.assignForReview(claimId, req.reviewerUserId(), auth.getName());
    }

    @PostMapping("/authenticity/claims/{claimId}/verify")
    @Operation(summary = "Verify authenticity claim (reviewer only)")
    public AuthenticityClaim verify(
            @PathVariable String claimId,
            @Valid @RequestBody ReviewDecisionRequest req,
            Authentication auth) {
        return service.verifyClaim(claimId, auth.getName(), req.notes());
    }

    @PostMapping("/authenticity/claims/{claimId}/reject")
    @Operation(summary = "Reject authenticity claim (reviewer only)")
    public AuthenticityClaim rejectClaim(
            @PathVariable String claimId,
            @Valid @RequestBody ReviewDecisionRequest req,
            Authentication auth) {
        return service.rejectClaim(claimId, auth.getName(), req.reason());
    }

    @PostMapping("/authenticity/claims/{claimId}/revoke")
    @Operation(summary = "Revoke a verified authenticity claim (admin only)")
    public AuthenticityClaim revoke(
            @PathVariable String claimId,
            @Valid @RequestBody ReviewDecisionRequest req,
            Authentication auth) {
        return service.revokeClaim(claimId, auth.getName(), req.reason());
    }

    // ---- Copyright Claims ---------------------------------------------------

    @PostMapping("/copyright/claims")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "File a copyright infringement claim")
    public CopyrightClaim fileCopyrightClaim(
            @Valid @RequestBody CopyrightClaimRequest req,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication auth) {
        return service.fileCopyrightClaim(auth.getName(), req.targetType(), req.targetId(),
                req.contentCreatorId(), req.claimDetails(), req.proofUrl(), correlationId);
    }

    @GetMapping("/copyright/claims/{claimId}")
    @Operation(summary = "Get copyright claim by ID")
    public CopyrightClaim getCopyrightClaim(@PathVariable String claimId) {
        return service.getCopyrightClaim(claimId);
    }

    @PostMapping("/copyright/claims/{claimId}/respond")
    @Operation(summary = "Creator responds to a copyright claim")
    public CopyrightClaim respond(
            @PathVariable String claimId,
            @Valid @RequestBody CreatorResponseRequest req,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication auth) {
        return service.respondToClaim(claimId, auth.getName(), req.response(), correlationId);
    }

    @PostMapping("/copyright/claims/{claimId}/uphold")
    @Operation(summary = "Uphold copyright claim (content stays removed)")
    public CopyrightClaim uphold(
            @PathVariable String claimId,
            @Valid @RequestBody ReviewDecisionRequest req,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication auth) {
        return service.upholdClaim(claimId, auth.getName(), req.reason(), correlationId);
    }

    @PostMapping("/copyright/claims/{claimId}/dismiss")
    @Operation(summary = "Dismiss copyright claim and restore content")
    public CopyrightClaim dismiss(
            @PathVariable String claimId,
            @Valid @RequestBody ReviewDecisionRequest req,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication auth) {
        return service.dismissClaim(claimId, auth.getName(), req.reason(), correlationId);
    }

    // ---- Sensitive Content --------------------------------------------------

    @PostMapping("/sensitive-content/flags")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Flag content as SACRED or COMMUNITY_RESTRICTED")
    public SensitiveContentFlag flagContent(
            @Valid @RequestBody SensitiveFlagRequest req,
            Authentication auth) {
        return service.flagSensitiveContent(req.targetType(), req.targetId(),
                req.flagType(), req.reason(), auth.getName());
    }

    @PostMapping("/sensitive-content/flags/{flagId}/approve")
    @Operation(summary = "Approve a sensitive content flag (admin only)")
    public SensitiveContentFlag approveFlag(
            @PathVariable String flagId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication auth) {
        return service.approveSensitiveFlag(flagId, auth.getName(), correlationId);
    }

    @GetMapping("/sensitive-content/check")
    @Operation(summary = "Check whether a content item is blocked from public feed")
    public BlockedStatusResponse isBlocked(
            @RequestParam TargetType targetType,
            @RequestParam String targetId) {
        boolean blocked = service.isBlockedFromPublicFeed(targetType, targetId);
        return new BlockedStatusResponse(targetType, targetId, blocked);
    }

    // ---- Reviewers ----------------------------------------------------------

    @GetMapping("/reviewers/eligible")
    @Operation(summary = "Find eligible reviewers for a given content type")
    public List<CulturalReviewer> eligibleReviewers(
            @RequestParam TargetType contentType) {
        return service.findEligibleReviewers(contentType);
    }

    // ---- DTOs ---------------------------------------------------------------

    public record AuthenticityClaimRequest(
            @NotNull TargetType targetType,
            @NotBlank @Size(max = 120) String targetId,
            @Size(max = 2000) String description) {}

    public record EvidenceRequest(
            @NotBlank @Size(max = 50) String evidenceType,
            @NotBlank @Size(max = 500) String mediaUrl,
            @Size(max = 1000) String description) {}

    public record AssignReviewerRequest(
            @NotBlank @Size(max = 120) String reviewerUserId) {}

    public record ReviewDecisionRequest(
            @Size(max = 2000) String notes,
            @Size(max = 2000) String reason) {}

    public record CopyrightClaimRequest(
            @NotNull TargetType targetType,
            @NotBlank @Size(max = 120) String targetId,
            @Size(max = 120) String contentCreatorId,
            @NotBlank @Size(max = 5000) String claimDetails,
            @Size(max = 500) String proofUrl) {}

    public record CreatorResponseRequest(
            @NotBlank @Size(max = 5000) String response) {}

    public record SensitiveFlagRequest(
            @NotNull TargetType targetType,
            @NotBlank @Size(max = 120) String targetId,
            @NotNull SensitiveContentFlag.FlagType flagType,
            @Size(max = 2000) String reason) {}

    public record BlockedStatusResponse(
            TargetType targetType, String targetId, boolean blockedFromPublicFeed) {}
}
