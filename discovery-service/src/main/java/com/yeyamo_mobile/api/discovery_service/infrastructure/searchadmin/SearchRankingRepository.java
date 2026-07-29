package com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin;
import java.util.*;import org.springframework.data.jpa.repository.*;
public interface SearchRankingRepository extends JpaRepository<SearchRankingEntity,UUID>{@Query("select r from SearchRankingEntity r where r.active=true")Optional<SearchRankingEntity> findActive();@Query("select coalesce(max(r.version),0) from SearchRankingEntity r")int maxVersion();}
