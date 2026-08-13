package com.yeyamo_mobile.api.country_config_service.domain.repository;

import com.yeyamo_mobile.api.country_config_service.domain.model.CountryCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CountryCurrencyRepository extends JpaRepository<CountryCurrency, UUID> {

    List<CountryCurrency> findByCountryId(UUID countryId);

    List<CountryCurrency> findByCountryIdAndIsDefaultTrue(UUID countryId);
}
