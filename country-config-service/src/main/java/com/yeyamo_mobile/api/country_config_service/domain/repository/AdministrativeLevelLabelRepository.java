package com.yeyamo_mobile.api.country_config_service.domain.repository;

import com.yeyamo_mobile.api.country_config_service.domain.model.AdministrativeLevelLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdministrativeLevelLabelRepository extends JpaRepository<AdministrativeLevelLabel, UUID> {

    List<AdministrativeLevelLabel> findByCountryCodeOrderByDisplayOrderAsc(String countryCode);

    Optional<AdministrativeLevelLabel> findByCountryCodeAndLevel(String countryCode, Integer level);

    boolean existsByCountryCodeAndLevel(String countryCode, Integer level);
}
