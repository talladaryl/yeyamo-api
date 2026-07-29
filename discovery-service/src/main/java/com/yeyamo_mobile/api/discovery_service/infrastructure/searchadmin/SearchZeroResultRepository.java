package com.yeyamo_mobile.api.discovery_service.infrastructure.searchadmin;
import java.util.*;import org.springframework.data.domain.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface SearchZeroResultRepository extends JpaRepository<SearchZeroResultEntity,String>{Page<SearchZeroResultEntity> findAllByOrderByOccurrencesDesc(Pageable pageable);}
