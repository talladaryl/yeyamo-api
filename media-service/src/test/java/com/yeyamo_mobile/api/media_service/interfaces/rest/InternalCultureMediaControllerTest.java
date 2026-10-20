package com.yeyamo_mobile.api.media_service.interfaces.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.yeyamo_mobile.api.media_service.domain.model.*;
import com.yeyamo_mobile.api.media_service.domain.port.MediaRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

class InternalCultureMediaControllerTest {
    private final MediaRepository repository=mock(MediaRepository.class);
    private final InternalCultureMediaController controller=new InternalCultureMediaController(repository);

    @Test void acceptsReadyOwnedRecipeHero(){
        UUID id=UUID.randomUUID();MediaAsset media=asset(id,"user-1",MediaStatus.READY,MediaType.IMAGE,MediaUsageType.RECIPE_HERO_IMAGE);
        when(repository.findById(id)).thenReturn(Optional.of(media));
        var response=controller.validate(new InternalCultureMediaController.Request("user-1",List.of(new InternalCultureMediaController.Reference(id,InternalCultureMediaController.Usage.HERO)),true),auth("user-1"));
        assertTrue(response.results().getFirst().allowed());assertTrue(response.results().getFirst().publicationAllowed());
    }
    @Test void rejectsForeignOwnerWithoutDisclosingOwner(){
        UUID id=UUID.randomUUID();when(repository.findById(id)).thenReturn(Optional.of(asset(id,"other",MediaStatus.READY,MediaType.IMAGE,MediaUsageType.RECIPE_HERO_IMAGE)));
        var result=controller.validate(new InternalCultureMediaController.Request("user-1",List.of(new InternalCultureMediaController.Reference(id,InternalCultureMediaController.Usage.HERO)),false),auth("user-1")).results().getFirst();
        assertFalse(result.allowed());assertEquals("MEDIA_OWNER_MISMATCH",result.reason());
    }
    @Test void rejectsProcessingDeletedWrongTypeAndWrongUsage(){
        for(var fixture:List.of(
            asset(UUID.randomUUID(),"u",MediaStatus.PROCESSING,MediaType.IMAGE,MediaUsageType.RECIPE_HERO_IMAGE),
            asset(UUID.randomUUID(),"u",MediaStatus.DELETED,MediaType.IMAGE,MediaUsageType.RECIPE_HERO_IMAGE),
            asset(UUID.randomUUID(),"u",MediaStatus.READY,MediaType.VIDEO,MediaUsageType.RECIPE_VIDEO),
            asset(UUID.randomUUID(),"u",MediaStatus.READY,MediaType.IMAGE,MediaUsageType.POST_IMAGE))){
            when(repository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
            var result=controller.validate(new InternalCultureMediaController.Request("u",List.of(new InternalCultureMediaController.Reference(fixture.getId(),InternalCultureMediaController.Usage.HERO)),false),auth("u")).results().getFirst();assertFalse(result.allowed());
        }
    }
    @Test void rejectsContributorClaimMismatch(){
        UUID id=UUID.randomUUID();var result=controller.validate(new InternalCultureMediaController.Request("client-value",List.of(new InternalCultureMediaController.Reference(id,InternalCultureMediaController.Usage.HERO)),false),auth("authenticated-value")).results().getFirst();
        assertFalse(result.allowed());assertEquals("CONTRIBUTOR_CONTEXT_MISMATCH",result.reason());verifyNoInteractions(repository);
    }
    private UsernamePasswordAuthenticationToken auth(String actor){Jwt jwt=Jwt.withTokenValue("signed").header("alg","HS256").subject("culture-service").claim("act",actor).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();return new UsernamePasswordAuthenticationToken(jwt,"signed",List.of());}
    private MediaAsset asset(UUID id,String owner,MediaStatus status,MediaType type,MediaUsageType usage){MediaAsset m=new MediaAsset();m.setId(id);m.setOwnerId(owner);m.setStatus(status);m.setType(type);m.setUsageType(usage);m.setContentType(type==MediaType.VIDEO?"video/mp4":"image/jpeg");m.setConsentStatus(ConsentStatus.NOT_REQUIRED);return m;}
}
