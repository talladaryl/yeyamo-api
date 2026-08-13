package com.yeyamo_mobile.api.country_config_service.domain.repository;

import com.yeyamo_mobile.api.country_config_service.domain.model.Locality;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocalityRepository extends JpaRepository<Locality, UUID> {

    List<Locality> findByCityIdAndActiveTrue(UUID cityId);

    Page<Locality> findByCityIdAndActiveTrue(UUID cityId, Pageable pageable);

    List<Locality> findByAdministrativeAreaIdAndActiveTrue(UUID administrativeAreaId);

    Page<Locality> findByAdministrativeAreaIdAndActiveTrue(UUID administrativeAreaId, Pageable pageable);

    List<Locality> findByCountryCodeAndActiveTrue(String countryCode);

    Page<Locality> findByCountryCodeAndActiveTrue(String countryCode, Pageable pageable);

    Optional<Locality> findByCountryCodeAndSlug(String countryCode, String slug);

    boolean existsByCountryCodeAndSlug(String countryCode, String slug);
}
