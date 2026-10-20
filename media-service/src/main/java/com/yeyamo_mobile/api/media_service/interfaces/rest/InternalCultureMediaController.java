package com.yeyamo_mobile.api.media_service.interfaces.rest;

import com.yeyamo_mobile.api.media_service.domain.model.*;
import com.yeyamo_mobile.api.media_service.domain.port.MediaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/service/v1/media/culture")
public class InternalCultureMediaController {
    private final MediaRepository repository;
    public InternalCultureMediaController(MediaRepository repository) { this.repository = repository; }

    public record Reference(@NotNull UUID mediaId, @NotNull Usage usage) {}
    public enum Usage { HERO, VIDEO, GALLERY, STEP }
    public record Request(@NotBlank String contributorId, @NotEmpty List<@Valid Reference> references, boolean publication) {}
    public record Result(UUID mediaId, boolean allowed, String reason, boolean publicationAllowed) {}
    public record Response(List<Result> results) {}

    @PostMapping("/validate")
    public Response validate(@Valid @RequestBody Request request, org.springframework.security.core.Authentication auth) {
        Jwt serviceJwt = (Jwt) auth.getPrincipal();
        String authenticatedActor = serviceJwt.getClaimAsString("act");
        if (authenticatedActor == null || !authenticatedActor.equals(request.contributorId())) {
            return denied(request.references(), "CONTRIBUTOR_CONTEXT_MISMATCH");
        }
        return new Response(request.references().stream().map(ref -> validateOne(ref, authenticatedActor)).toList());
    }

    private Result validateOne(Reference ref, String actor) {
        MediaAsset media = repository.findById(ref.mediaId()).orElse(null);
        if (media == null) return result(ref, false, "MEDIA_NOT_FOUND", false);
        if (!actor.equals(media.getOwnerId())) return result(ref, false, "MEDIA_OWNER_MISMATCH", false);
        if (media.getStatus() == MediaStatus.DELETED) return result(ref, false, "MEDIA_DELETED", false);
        if (media.getStatus() != MediaStatus.READY) return result(ref, false, "MEDIA_NOT_READY", false);
        MediaType expected = ref.usage() == Usage.VIDEO ? MediaType.VIDEO : MediaType.IMAGE;
        if (media.getType() != expected || media.getContentType() == null ||
                !(media.getContentType().startsWith("image/") || media.getContentType().startsWith("video/")))
            return result(ref, false, "MEDIA_TYPE_NOT_ALLOWED", false);
        if (media.getConsentStatus() == ConsentStatus.REFUSED || media.getConsentStatus() == ConsentStatus.PENDING)
            return result(ref, false, "MEDIA_CONSENT_NOT_GRANTED", false);
        MediaUsageType required = switch (ref.usage()) {
            case HERO -> MediaUsageType.RECIPE_HERO_IMAGE;
            case VIDEO -> MediaUsageType.RECIPE_VIDEO;
            case GALLERY -> MediaUsageType.RECIPE_GALLERY_IMAGE;
            case STEP -> MediaUsageType.RECIPE_STEP_IMAGE;
        };
        if (media.getUsageType() != required) return result(ref, false, "MEDIA_USAGE_NOT_ALLOWED", false);
        return result(ref, true, "ALLOWED", true);
    }

    private Result result(Reference ref, boolean allowed, String reason, boolean publish) {
        return new Result(ref.mediaId(), allowed, reason, publish);
    }
    private Response denied(List<Reference> refs, String reason) {
        return new Response(refs.stream().map(ref -> result(ref, false, reason, false)).toList());
    }
}
