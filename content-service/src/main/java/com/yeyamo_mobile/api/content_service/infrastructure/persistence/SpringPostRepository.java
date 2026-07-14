package com.yeyamo_mobile.api.content_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.domain.Pageable;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
import com.yeyamo_mobile.api.content_service.domain.model.*;
public interface SpringPostRepository extends JpaRepository<PostEntity,UUID>{
 List<PostEntity> findByAuthorIdAndStatusNotOrderByUpdatedAtDesc(String authorId,PostStatus status,Pageable page);
 @Query("select distinct p from PostEntity p join p.hashtags h where h=:tag and p.status=:status and p.visibility=:visibility order by p.publishedAt desc")
 List<PostEntity> byHashtag(@Param("tag")String tag,@Param("status")PostStatus status,@Param("visibility")PostVisibility visibility,Pageable page);
 List<PostEntity> findByCatalogAssetIdAndStatusAndVisibilityOrderByPublishedAtDesc(UUID assetId,PostStatus status,PostVisibility visibility,Pageable page);
}
