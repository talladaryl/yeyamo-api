package com.yeyamo_mobile.api.place_service.repository;

import com.yeyamo_mobile.api.place_service.models.CountryLanguage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryLanguageRepository extends JpaRepository<CountryLanguage, Long> {
    List<CountryLanguage> findByCountry_CodeOrderByPrimaryLanguageDescDisplayNameAsc(String countryCode);
}
