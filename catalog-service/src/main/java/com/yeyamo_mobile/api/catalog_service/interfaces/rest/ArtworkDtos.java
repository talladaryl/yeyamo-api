package com.yeyamo_mobile.api.catalog_service.interfaces.rest;

import com.yeyamo_mobile.api.catalog_service.infrastructure.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;

public final class ArtworkDtos { private ArtworkDtos() {}
    public record Translation(@Pattern(regexp="^[A-Za-z]{2,3}([_-][A-Za-z0-9]{2,8})*$")String languageCode,@NotBlank String title,String shortDescription,String story,ArtworkDetails.TranslationStatus status,String translatorId){}
    public record Media(@NotNull UUID mediaId,@NotNull ArtworkDetails.MediaType type,@PositiveOrZero int displayOrder){}
    public record Request(@NotNull UUID artisanPartnerId,@NotBlank String title,String slug,String shortDescription,String story,
        @Pattern(regexp="^[A-Z]{2}$")String countryCode,String adminLevel1Id,String cityId,String localityId,UUID cultureContentId,
        String culturalCommunity,@Min(0)Integer yearCreated,String productionTime,@PositiveOrZero BigDecimal width,@PositiveOrZero BigDecimal height,
        @PositiveOrZero BigDecimal depth,@PositiveOrZero BigDecimal weight,@NotNull ArtworkEntity.EditionType editionType,@Positive Integer editionSize,
        @NotNull ArtworkEntity.AvailabilityStatus availabilityStatus,List<Translation> translations,List<Media> mediaIds){}
    public record Response(ArtworkEntity artwork,List<ArtworkDetails.Translation> translations,List<ArtworkDetails.Media> media){}
    public record HistoryRequest(@NotBlank String title,@NotBlank String narrative,@NotBlank String languageCode,String period,String culturalMeaning,String source,String contributorId,ArtworkDetails.VerificationStatus verificationStatus){}
    public record AvailabilityRequest(@NotNull ArtworkEntity.AvailabilityStatus status){}
}
