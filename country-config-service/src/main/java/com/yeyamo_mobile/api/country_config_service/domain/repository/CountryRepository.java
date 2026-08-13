package com.yeyamo_mobile.api.country_config_service.domain.repository;

import com.yeyamo_mobile.api.country_config_service.domain.model.Country;
import com.yeyamo_mobile.api.country_config_service.domain.model.CountryLaunchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CountryRepository extends JpaRepository<Country, UUID> {

    Optional<Country> findByCode(String code);

    List<Country> findByLaunchStatus(CountryLaunchStatus launchStatus);

    @Query("SELECT c FROM Country c WHERE c.launchStatus IN ('LIVE', 'BETA')")
    List<Country> findAvailableCountries();

    @Query("SELECT c FROM Country c WHERE c.launchStatus = 'LIVE' AND c.registrationEnabled = true")
    List<Country> findCountriesAcceptingRegistration();

    boolean existsByCode(String code);
}
