package com.yeyamo_mobile.api.discovery_service.infrastructure.postgis;

import java.util.*;
import java.time.Instant;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SpringDiscoveryDocumentRepository extends JpaRepository<DiscoveryDocumentEntity,UUID> {
    Optional<DiscoveryDocumentEntity> findBySourceId(String id);

    @Modifying
    @Query("update DiscoveryDocumentEntity d set d.trendScore=case when d.trendScore+:delta<0 then 0 else d.trendScore+:delta end,d.updatedAt=:now where d.sourceId=:id")
    int adjust(@Param("id") String id,@Param("delta") double delta,@Param("now") Instant now);

    @Query(value = """
            select d.* from discovery_documents d
            where d.active=true
              and (:type is null or d.type=:type)
              and (:category is null or d.category_code=:category)
              and (:region is null or d.region_code=:region)
              and (:query is null or d.search_vector @@ websearch_to_tsquery('simple',:query))
            order by
              case when :trends then d.trend_score/power(1+greatest(0,extract(epoch from(now()-coalesce(d.published_at,d.updated_at)))/86400),1.2) else 0 end desc,
              case when :query is not null then ts_rank(d.search_vector,websearch_to_tsquery('simple',:query)) else 0 end desc,
              d.published_at desc nulls last
            limit :limit offset :offset
            """,nativeQuery=true)
    List<DiscoveryDocumentEntity> search(@Param("query") String query,@Param("type") String type,@Param("category") String category,
            @Param("region") String region,@Param("trends") boolean trends,@Param("limit") int limit,@Param("offset") int offset);

    @Query(value = """
            select d.* from discovery_documents d
            where d.active=true and d.location is not null
              and (:type is null or d.type=:type)
              and (:category is null or d.category_code=:category)
              and (:region is null or d.region_code=:region)
              and (:query is null or d.search_vector @@ websearch_to_tsquery('simple',:query))
              and ST_DWithin(d.location::geography,ST_SetSRID(ST_MakePoint(:lng,:lat),4326)::geography,:meters)
            order by
              case when :trends then d.trend_score/power(1+greatest(0,extract(epoch from(now()-coalesce(d.published_at,d.updated_at)))/86400),1.2) else 0 end desc,
              ST_Distance(d.location::geography,ST_SetSRID(ST_MakePoint(:lng,:lat),4326)::geography),
              d.published_at desc nulls last
            limit :limit offset :offset
            """,nativeQuery=true)
    List<DiscoveryDocumentEntity> searchGeo(@Param("query") String query,@Param("type") String type,@Param("category") String category,
            @Param("region") String region,@Param("trends") boolean trends,@Param("lat") double lat,@Param("lng") double lng,
            @Param("meters") double meters,@Param("limit") int limit,@Param("offset") int offset);
}
