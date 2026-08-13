package com.yeyamo_mobile.api.country_config_service.domain.repository;

import com.yeyamo_mobile.api.country_config_service.domain.model.CountryLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CountryLanguageRepository extends JpaRepository<CountryLanguage, UUID> {

    List<CountryLanguage> findByCountryIdOrderByDisplayOrderAsc(UUID countryId);

    List<CountryLanguage> findByCountryIdAndIsDefaultTrue(UUID countryId);
}
