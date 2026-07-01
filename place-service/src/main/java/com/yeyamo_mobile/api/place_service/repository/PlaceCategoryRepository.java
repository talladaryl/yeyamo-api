package com.yeyamo_mobile.api.place_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yeyamo_mobile.api.place_service.models.PlaceCategory;

public interface PlaceCategoryRepository extends JpaRepository<PlaceCategory, Long> {

    Optional<PlaceCategory> findBySlug(String slug);

    List<PlaceCategory> findByParentIsNullOrderByNameAsc();
}
