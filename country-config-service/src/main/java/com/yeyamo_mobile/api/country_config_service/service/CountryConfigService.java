package com.yeyamo_mobile.api.country_config_service.service;

import com.yeyamo_mobile.api.country_config_service.domain.model.*;
import com.yeyamo_mobile.api.country_config_service.domain.repository.*;
import com.yeyamo_mobile.api.country_config_service.dto.*;
import com.yeyamo_mobile.api.country_config_service.event.CountryEventPublisher;
import com.yeyamo_mobile.api.country_config_service.mapper.CountryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CountryConfigService {

    private static final Logger log = LoggerFactory.getLogger(CountryConfigService.class);

    private final CountryRepository countryRepository;
    private final CountryLanguageRepository languageRepository;
    private final CountryCurrencyRepository currencyRepository;
    private final CountryTimezoneRepository timezoneRepository;
    private final CountryEventPublisher eventPublisher;
    private final CountryMapper mapper;

    public CountryConfigService(
            CountryRepository countryRepository,
            CountryLanguageRepository languageRepository,
            CountryCurrencyRepository currencyRepository,
            CountryTimezoneRepository timezoneRepository,
            CountryEventPublisher eventPublisher,
            CountryMapper mapper
    ) {
        this.countryRepository = countryRepository;
        this.languageRepository = languageRepository;
        this.currencyRepository = currencyRepository;
        this.timezoneRepository = timezoneRepository;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    @Cacheable(value = "countries", key = "'all'")
    public List<CountryDto> getAllCountries() {
        log.debug("Fetching all countries");
        return countryRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Cacheable(value = "countries", key = "'available'")
    public List<CountryDto> getAvailableCountries() {
        log.debug("Fetching available countries (LIVE or BETA)");
        return countryRepository.findAvailableCountries().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Cacheable(value = "country", key = "#code")
    public CountryDto getCountryByCode(String code) {
        log.debug("Fetching country: {}", code);
        Country country = countryRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new CountryNotFoundException("Country not found: " + code));
        return mapper.toDto(country);
    }

    @Cacheable(value = "country-config", key = "#code")
    public CountryConfigurationDto getCountryConfiguration(String code) {
        log.debug("Fetching full configuration for country: {}", code);
        
        Country country = countryRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new CountryNotFoundException("Country not found: " + code));

        List<CountryLanguage> languages = languageRepository.findByCountryIdOrderByDisplayOrderAsc(country.getId());
        List<CountryCurrency> currencies = currencyRepository.findByCountryId(country.getId());
        List<CountryTimezone> timezones = timezoneRepository.findByCountryId(country.getId());

        return new CountryConfigurationDto(
                mapper.toDto(country),
                languages.stream().map(mapper::toDto).toList(),
                currencies.stream().map(mapper::toDto).toList(),
                timezones.stream().map(mapper::toDto).toList()
        );
    }

    @Cacheable(value = "country-features", key = "#code")
    public FeatureFlagsDto getCountryFeatures(String code) {
        log.debug("Fetching features for country: {}", code);
        
        Country country = countryRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new CountryNotFoundException("Country not found: " + code));

        return new FeatureFlagsDto(
                country.getRegistrationEnabled(),
                country.getContentPublishingEnabled(),
                country.getPartnerOnboardingEnabled(),
                country.getPaymentsEnabled(),
                country.getBookingEnabled(),
                country.getTicketingEnabled(),
                country.getArtisanCommerceEnabled(),
                country.getCultureModuleEnabled()
        );
    }

    public List<LanguageDto> getCountryLanguages(String code) {
        Country country = countryRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new CountryNotFoundException("Country not found: " + code));
        
        return languageRepository.findByCountryIdOrderByDisplayOrderAsc(country.getId()).stream()
                .map(mapper::toDto)
                .toList();
    }

    public List<CurrencyDto> getCountryCurrencies(String code) {
        Country country = countryRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new CountryNotFoundException("Country not found: " + code));
        
        return currencyRepository.findByCountryId(country.getId()).stream()
                .map(mapper::toDto)
                .toList();
    }

    public List<TimezoneDto> getCountryTimezones(String code) {
        Country country = countryRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new CountryNotFoundException("Country not found: " + code));
        
        return timezoneRepository.findByCountryId(country.getId()).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"country", "country-config", "country-features", "countries"}, allEntries = true)
    public CountryDto updateCountry(String code, UpdateCountryRequest request) {
        log.info("Updating country: {}", code);
        
        Country country = countryRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new CountryNotFoundException("Country not found: " + code));

        country.setName(request.name());
        country.setOfficialName(request.officialName());
        country.setDefaultLanguageCode(request.defaultLanguageCode());
        country.setDefaultCurrencyCode(request.defaultCurrencyCode());
        country.setDefaultTimezone(request.defaultTimezone());
        country.setPhoneCountryCode(request.phoneCountryCode());

        Country saved = countryRepository.save(country);
        
        eventPublisher.publishCountryConfigurationUpdated(saved);
        
        return mapper.toDto(saved);
    }

    @Transactional
    @CacheEvict(value = {"country", "country-config", "countries"}, allEntries = true)
    public CountryDto updateLaunchStatus(String code, CountryLaunchStatus newStatus) {
        log.info("Updating launch status for country {} to {}", code, newStatus);
        
        Country country = countryRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new CountryNotFoundException("Country not found: " + code));

        CountryLaunchStatus oldStatus = country.getLaunchStatus();
        country.updateLaunchStatus(newStatus);

        Country saved = countryRepository.save(country);
        
        eventPublisher.publishCountryLaunchStatusChanged(saved, oldStatus, newStatus);
        
        return mapper.toDto(saved);
    }

    @Transactional
    @CacheEvict(value = {"country", "country-config", "country-features"}, allEntries = true)
    public CountryDto updateFeatures(String code, UpdateFeaturesRequest request) {
        log.info("Updating features for country: {}", code);
        
        Country country = countryRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new CountryNotFoundException("Country not found: " + code));

        country.setRegistrationEnabled(request.registrationEnabled());
        country.setContentPublishingEnabled(request.contentPublishingEnabled());
        country.setPartnerOnboardingEnabled(request.partnerOnboardingEnabled());
        country.setPaymentsEnabled(request.paymentsEnabled());
        country.setBookingEnabled(request.bookingEnabled());
        country.setTicketingEnabled(request.ticketingEnabled());
        country.setArtisanCommerceEnabled(request.artisanCommerceEnabled());
        country.setCultureModuleEnabled(request.cultureModuleEnabled());

        Country saved = countryRepository.save(country);
        
        eventPublisher.publishCountryFeatureChanged(saved);
        
        return mapper.toDto(saved);
    }

    public static class CountryNotFoundException extends RuntimeException {
        public CountryNotFoundException(String message) {
            super(message);
        }
    }
}
