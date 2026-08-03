package com.yeyamo_mobile.api.place_service.service;

import com.yeyamo.foundation.domain.CountryReference;
import com.yeyamo_mobile.api.place_service.dto.CountryLanguageResponse;
import com.yeyamo_mobile.api.place_service.dto.CountryResponse;
import com.yeyamo_mobile.api.place_service.dto.RegionResponse;
import com.yeyamo_mobile.api.place_service.exception.ApiException;
import com.yeyamo_mobile.api.place_service.models.Country;
import com.yeyamo_mobile.api.place_service.repository.CountryLanguageRepository;
import com.yeyamo_mobile.api.place_service.repository.CountryRepository;
import com.yeyamo_mobile.api.place_service.repository.RegionRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CountryService {
    private final CountryRepository countryRepository;
    private final CountryLanguageRepository languageRepository;
    private final RegionRepository regionRepository;

    public CountryService(CountryRepository countryRepository,
            CountryLanguageRepository languageRepository,
            RegionRepository regionRepository) {
        this.countryRepository = countryRepository;
        this.languageRepository = languageRepository;
        this.regionRepository = regionRepository;
    }

    public List<CountryResponse> list() {
        return countryRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(CountryResponse::from)
                .toList();
    }

    public CountryResponse get(String countryCode) {
        return CountryResponse.from(requireCountry(countryCode));
    }

    public List<RegionResponse> administrativeAreas(String countryCode) {
        Country country = requireCountry(countryCode);
        return regionRepository.findByCountryCodeAndActiveTrueOrderByNameAsc(country.getCode()).stream()
                .map(RegionResponse::from)
                .toList();
    }

    public List<CountryLanguageResponse> languages(String countryCode) {
        Country country = requireCountry(countryCode);
        return languageRepository.findByCountry_CodeOrderByPrimaryLanguageDescDisplayNameAsc(country.getCode()).stream()
                .map(CountryLanguageResponse::from)
                .toList();
    }

    private Country requireCountry(String value) {
        String code;
        try {
            code = new CountryReference(value).countryCode();
        } catch (IllegalArgumentException exception) {
            throw new ApiException("COUNTRY_CODE_INVALID", exception.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return countryRepository.findById(code)
                .filter(Country::isActive)
                .orElseThrow(() -> new ApiException("COUNTRY_NOT_FOUND", "Pays introuvable", HttpStatus.NOT_FOUND));
    }
}
