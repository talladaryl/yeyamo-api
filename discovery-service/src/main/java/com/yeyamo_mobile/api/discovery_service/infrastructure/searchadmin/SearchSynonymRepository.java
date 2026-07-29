package com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin;
import java.util.UUID;import org.springframework.data.jpa.repository.JpaRepository;
public interface SearchSynonymRepository extends JpaRepository<SearchSynonymEntity,UUID>{}
