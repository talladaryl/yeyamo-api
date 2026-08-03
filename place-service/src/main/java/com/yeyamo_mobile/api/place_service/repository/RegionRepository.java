package com.yeyamo_mobile.api.place_service.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.place_service.models.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {

    Optional<Region> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Region> findByCountryCodeAndActiveTrueOrderByNameAsc(String countryCode);
}
