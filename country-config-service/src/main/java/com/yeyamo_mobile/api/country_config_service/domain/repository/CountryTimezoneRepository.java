package com.yeyamo_mobile.api.country_config_service.domain.repository;

import com.yeyamo_mobile.api.country_config_service.domain.model.CountryTimezone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CountryTimezoneRepository extends JpaRepository<CountryTimezone, UUID> {

    List<CountryTimezone> findByCountryId(UUID countryId);

    List<CountryTimezone> findByCountryIdAndIsDefaultTrue(UUID countryId);
}
