package com.yeyamo_mobile.api.country_config_service.domain.repository;

import com.yeyamo_mobile.api.country_config_service.domain.model.City;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CityRepository extends JpaRepository<City, UUID> {

    List<City> findByCountryCodeAndActiveTrue(String countryCode);

    Page<City> findByCountryCodeAndActiveTrue(String countryCode, Pageable pageable);

    List<City> findByAdministrativeAreaIdAndActiveTrue(UUID administrativeAreaId);

    Page<City> findByAdministrativeAreaIdAndActiveTrue(UUID administrativeAreaId, Pageable pageable);

    Optional<City> findByCountryCodeAndSlug(String countryCode, String slug);

    boolean existsByCountryCodeAndSlug(String countryCode, String slug);
}
