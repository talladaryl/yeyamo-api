package com.yeyamo_mobile.api.catalog_service.infrastructure.persistence;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public final class ArtworkRepositories { private ArtworkRepositories() {}
    public interface Artworks extends JpaRepository<ArtworkEntity,UUID>,JpaSpecificationExecutor<ArtworkEntity>{
        Optional<ArtworkEntity> findByAssetIdAndDeletedAtIsNull(UUID id);
        Page<ArtworkEntity> findByArtisanPartnerIdAndDeletedAtIsNull(UUID artisanId,Pageable pageable);
        boolean existsBySlug(String slug);
        @Query("select a from ArtworkEntity a where a.assetId=:id and a.deletedAt is null") @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) Optional<ArtworkEntity> locked(@Param("id")UUID id);
    }
    public interface Translations extends JpaRepository<ArtworkDetails.Translation,UUID>{List<ArtworkDetails.Translation> findByArtworkId(UUID id);}
    public interface Histories extends JpaRepository<ArtworkDetails.History,UUID>{List<ArtworkDetails.History> findByArtworkIdOrderByCreatedAtAsc(UUID id);Optional<ArtworkDetails.History> findByIdAndArtworkId(UUID entryId,UUID artworkId);}
    public interface Media extends JpaRepository<ArtworkDetails.Media,ArtworkDetails.MediaId>{List<ArtworkDetails.Media> findByArtworkIdOrderByDisplayOrderAsc(UUID id);}
    public interface Materials extends JpaRepository<ArtworkReferenceEntities.Material,UUID>{boolean existsByCode(String code);}
    public interface MaterialTranslations extends JpaRepository<ArtworkReferenceEntities.MaterialTranslation,ArtworkReferenceEntities.MaterialTranslationId>{List<ArtworkReferenceEntities.MaterialTranslation>findByMaterialId(UUID id);}
    public interface Techniques extends JpaRepository<ArtworkReferenceEntities.Technique,UUID>{boolean existsByCode(String code);}
    public interface TechniqueTranslations extends JpaRepository<ArtworkReferenceEntities.TechniqueTranslation,ArtworkReferenceEntities.TechniqueTranslationId>{List<ArtworkReferenceEntities.TechniqueTranslation>findByTechniqueId(UUID id);}
}
