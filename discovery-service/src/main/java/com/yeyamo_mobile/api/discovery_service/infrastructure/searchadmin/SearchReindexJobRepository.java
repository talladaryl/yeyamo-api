package com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface SearchReindexJobRepository extends JpaRepository<SearchReindexJobEntity,UUID>{boolean existsByStatusIn(Collection<String> statuses);}
