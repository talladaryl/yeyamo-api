package com.yeyamo_mobile.api.discovery_service.infrastructure.postgis;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDiscoveryDocumentRepository extends JpaRepository<DiscoveryDocumentEntity, UUID> {
    Optional<DiscoveryDocumentEntity> findBySourceId(String id);

    @Modifying
    @Query("update DiscoveryDocumentEntity d set d.trendScore=case when d.trendScore+:delta<0 then 0 else d.trendScore+:delta end,d.updatedAt=:now where d.sourceId=:id")
    int adjust(@Param("id") String id, @Param("delta") double delta, @Param("now") Instant now);

    @Query(value = """
            select d.* from discovery_documents d
            where d.active=true
              and d.launch_visibility in ('PUBLIC', 'PUBLIC_HERITAGE')
              and (:type is null or d.type=:type)
              and (:category is null or d.category_code=:category)
              and (:region is null or d.region_code=:region)
              and (:country is null or d.country_code=:country)
              and (:countries is null or d.country_code = any(string_to_array(:countries, ',')))
              and (:admin_level1 is null or d.admin_level1_id=:admin_level1)
              and (:city_id is null or d.city_id=:city_id)
              and (:language is null or lower(:language) = any(string_to_array(lower(replace(coalesce(d.language_codes,''),' ','')), ',')))
              and (:culture_type is null or lower(d.type)=lower(:culture_type))
              and (:material is null or lower(:material) = any(string_to_array(lower(replace(coalesce(d.materials,''),' ','')), ',')))
              and (:technique is null or lower(:technique) = any(string_to_array(lower(replace(coalesce(d.techniques,''),' ','')), ',')))
              and (:availability is null or d.availability_status = case when :availability then 'AVAILABLE' else 'SOLD' end)
              and (:verified is null or not :verified or d.verification_status='VERIFIED')
              and (:query is null or d.search_vector @@ websearch_to_tsquery('simple',:query)
                   or d.title ilike '%' || :query || '%' or d.aliases ilike '%' || :query || '%')
            order by
              case when :trends then d.popularity_signal else 0 end desc,
              case when :trends then d.trend_score/power(1+greatest(0,extract(epoch from(now()-coalesce(d.published_at,d.updated_at)))/86400),1.2) else 0 end desc,
              case when :query is not null then ts_rank(d.search_vector,websearch_to_tsquery('simple',:query)) else 0 end desc,
              d.published_at desc nulls last
            limit :limit offset :offset
            """, nativeQuery = true)
    List<DiscoveryDocumentEntity> search(
            @Param("query") String query,
            @Param("type") String type,
            @Param("category") String category,
            @Param("region") String region,
            @Param("country") String country,
            @Param("countries") String countries,
            @Param("admin_level1") String adminLevel1,
            @Param("city_id") String cityId,
            @Param("language") String language,
            @Param("culture_type") String cultureType,
            @Param("material") String material,
            @Param("technique") String technique,
            @Param("availability") Boolean availability,
            @Param("verified") Boolean verified,
            @Param("trends") boolean trends,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Query(value = """
            select d.* from discovery_documents d
            where d.active=true
              and d.launch_visibility in ('PUBLIC', 'PUBLIC_HERITAGE')
              and d.location is not null
              and (:type is null or d.type=:type)
              and (:category is null or d.category_code=:category)
              and (:region is null or d.region_code=:region)
              and (:country is null or d.country_code=:country)
              and (:countries is null or d.country_code = any(string_to_array(:countries, ',')))
              and (:admin_level1 is null or d.admin_level1_id=:admin_level1)
              and (:city_id is null or d.city_id=:city_id)
              and (:language is null or lower(:language) = any(string_to_array(lower(replace(coalesce(d.language_codes,''),' ','')), ',')))
              and (:culture_type is null or lower(d.type)=lower(:culture_type))
              and (:material is null or lower(:material) = any(string_to_array(lower(replace(coalesce(d.materials,''),' ','')), ',')))
              and (:technique is null or lower(:technique) = any(string_to_array(lower(replace(coalesce(d.techniques,''),' ','')), ',')))
              and (:availability is null or d.availability_status = case when :availability then 'AVAILABLE' else 'SOLD' end)
              and (:verified is null or not :verified or d.verification_status='VERIFIED')
              and (:query is null or d.search_vector @@ websearch_to_tsquery('simple',:query)
                   or d.title ilike '%' || :query || '%' or d.aliases ilike '%' || :query || '%')
              and ST_DWithin(d.location::geography,ST_SetSRID(ST_MakePoint(:lng,:lat),4326)::geography,:meters)
            order by
              case when :trends then d.popularity_signal else 0 end desc,
              case when :trends then d.trend_score/power(1+greatest(0,extract(epoch from(now()-coalesce(d.published_at,d.updated_at)))/86400),1.2) else 0 end desc,
              case when :query is not null then ts_rank(d.search_vector,websearch_to_tsquery('simple',:query)) else 0 end desc,
              ST_Distance(d.location::geography,ST_SetSRID(ST_MakePoint(:lng,:lat),4326)::geography),
              d.published_at desc nulls last
            limit :limit offset :offset
            """, nativeQuery = true)
    List<DiscoveryDocumentEntity> searchGeo(
            @Param("query") String query,
            @Param("type") String type,
            @Param("category") String category,
            @Param("region") String region,
            @Param("country") String country,
            @Param("countries") String countries,
            @Param("admin_level1") String adminLevel1,
            @Param("city_id") String cityId,
            @Param("language") String language,
            @Param("culture_type") String cultureType,
            @Param("material") String material,
            @Param("technique") String technique,
            @Param("availability") Boolean availability,
            @Param("verified") Boolean verified,
            @Param("trends") boolean trends,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("meters") double meters,
            @Param("limit") int limit,
            @Param("offset") int offset);
}
