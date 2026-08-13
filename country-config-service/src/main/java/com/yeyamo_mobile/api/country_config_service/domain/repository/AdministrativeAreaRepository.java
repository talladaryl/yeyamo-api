package com.yeyamo_mobile.api.country_config_service.domain.repository;

import com.yeyamo_mobile.api.country_config_service.domain.model.AdministrativeArea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdministrativeAreaRepository extends JpaRepository<AdministrativeArea, UUID> {

    List<AdministrativeArea> findByCountryCodeAndActiveTrue(String countryCode);

    Page<AdministrativeArea> findByCountryCodeAndActiveTrue(String countryCode, Pageable pageable);

    List<AdministrativeArea> findByCountryCodeAndLevelAndActiveTrue(String countryCode, Integer level);

    Page<AdministrativeArea> findByCountryCodeAndLevelAndActiveTrue(String countryCode, Integer level, Pageable pageable);

    List<AdministrativeArea> findByParentIdAndActiveTrue(UUID parentId);

    @Query("SELECT a FROM AdministrativeArea a WHERE a.countryCode = :countryCode AND a.parentId IS NULL AND a.active = true")
    List<AdministrativeArea> findTopLevelByCountry(@Param("countryCode") String countryCode);

    @Query("SELECT a FROM AdministrativeArea a WHERE a.countryCode = :countryCode AND a.parentId IS NULL AND a.active = true")
    Page<AdministrativeArea> findTopLevelByCountry(@Param("countryCode") String countryCode, Pageable pageable);

    Optional<AdministrativeArea> findByCountryCodeAndSlug(String countryCode, String slug);

    boolean existsByCountryCodeAndSlug(String countryCode, String slug);

    @Query("SELECT COUNT(a) FROM AdministrativeArea a WHERE a.countryCode = :countryCode AND a.level = :level AND a.active = true")
    long countByCountryAndLevel(@Param("countryCode") String countryCode, @Param("level") Integer level);
}
