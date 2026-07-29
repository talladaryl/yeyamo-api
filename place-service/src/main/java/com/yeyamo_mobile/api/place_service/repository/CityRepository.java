package com.yeyamo_mobile.api.place_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.place_service.models.City;

public interface CityRepository extends JpaRepository<City, Long> {

    List<City> findByRegionId(Long regionId);

    Optional<City> findByRegionIdAndSlug(Long regionId, String slug);

    boolean existsByRegionIdAndSlug(Long regionId, String slug);
    boolean existsByRegionId(Long regionId);
}
