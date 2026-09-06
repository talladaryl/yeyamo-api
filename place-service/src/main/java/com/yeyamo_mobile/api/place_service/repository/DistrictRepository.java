package com.yeyamo_mobile.api.place_service.repository;

import java.util.UUID;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.place_service.models.District;

public interface DistrictRepository extends JpaRepository<District, Long> {

    List<District> findByCityId(UUID cityId);
    boolean existsByCityIdAndNameIgnoreCase(UUID cityId,String name);
    boolean existsByCityId(UUID cityId);
}
