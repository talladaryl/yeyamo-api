package com.yeyamo_mobile.api.country_config_service.service;

import com.yeyamo_mobile.api.country_config_service.domain.model.*;
import com.yeyamo_mobile.api.country_config_service.domain.repository.*;
import com.yeyamo_mobile.api.country_config_service.dto.*;
import com.yeyamo_mobile.api.country_config_service.mapper.GeographyMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing generic geographic/administrative entities.
 * Supports diverse African administrative structures.
 */
@Service
@Transactional(readOnly = true)
public class GeographyService {

    private static final Logger log = LoggerFactory.getLogger(GeographyService.class);

    private final AdministrativeAreaRepository administrativeAreaRepository;
    private final AdministrativeLevelLabelRepository levelLabelRepository;
    private final CityRepository cityRepository;
    private final LocalityRepository localityRepository;
    private final CountryRepository countryRepository;
    private final GeographyMapper mapper;

    public GeographyService(
            AdministrativeAreaRepository administrativeAreaRepository,
            AdministrativeLevelLabelRepository levelLabelRepository,
            CityRepository cityRepository,
            LocalityRepository localityRepository,
            CountryRepository countryRepository,
            GeographyMapper mapper
    ) {
        this.administrativeAreaRepository = administrativeAreaRepository;
        this.levelLabelRepository = levelLabelRepository;
        this.cityRepository = cityRepository;
        this.localityRepository = localityRepository;
        this.countryRepository = countryRepository;
        this.mapper = mapper;
    }

    // ===========================
    // Administrative Areas
    // ===========================

    @Cacheable(value = "admin-areas", key = "#countryCode")
    public List<AdministrativeAreaDto> getAdministrativeAreas(String countryCode) {
        log.debug("Fetching administrative areas for country: {}", countryCode);
        validateCountry(countryCode);
        
        return administrativeAreaRepository.findByCountryCodeAndActiveTrue(countryCode.toUpperCase()).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Cacheable(value = "admin-areas-paged", key = "#countryCode + '-' + #pageable.pageNumber")
    public Page<AdministrativeAreaDto> getAdministrativeAreas(String countryCode, Pageable pageable) {
        log.debug("Fetching administrative areas for country: {} (page {})", countryCode, pageable.pageNumber());
        validateCountry(countryCode);
        
        return administrativeAreaRepository.findByCountryCodeAndActiveTrue(countryCode.toUpperCase(), pageable)
                .map(mapper::toDto);
    }

    @Cacheable(value = "admin-area-children", key = "#id")
    public List<AdministrativeAreaDto> getChildren(UUID id) {
        log.debug("Fetching children of administrative area: {}", id);
        
        return administrativeAreaRepository.findByParentIdAndActiveTrue(id).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Cacheable(value = "admin-area-top-level", key = "#countryCode")
    public List<AdministrativeAreaDto> getTopLevelAreas(String countryCode) {
        log.debug("Fetching top-level areas for country: {}", countryCode);
        validateCountry(countryCode);
        
        return administrativeAreaRepository.findTopLevelByCountry(countryCode.toUpperCase()).stream()
                .map(mapper::toDto)
                .toList();
    }

    public Page<AdministrativeAreaDto> getTopLevelAreas(String countryCode, Pageable pageable) {
        log.debug("Fetching top-level areas for country: {} (page {})", countryCode, pageable.pageNumber());
        validateCountry(countryCode);
        
        return administrativeAreaRepository.findTopLevelByCountry(countryCode.toUpperCase(), pageable)
                .map(mapper::toDto);
    }

    @Cacheable(value = "admin-area", key = "#id")
    public AdministrativeAreaDto getAdministrativeArea(UUID id) {
        AdministrativeArea area = administrativeAreaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administrative area not found: " + id));
        return mapper.toDto(area);
    }

    // ===========================
    // Administrative Level Labels
    // ===========================

    @Cacheable(value = "admin-labels", key = "#countryCode")
    public List<AdministrativeLevelLabelDto> getLevelLabels(String countryCode) {
        log.debug("Fetching level labels for country: {}", countryCode);
        validateCountry(countryCode);
        
        return levelLabelRepository.findByCountryCodeOrderByDisplayOrderAsc(countryCode.toUpperCase()).stream()
                .map(mapper::toDto)
                .toList();
    }

    // ===========================
    // Cities
    // ===========================

    @Cacheable(value = "cities", key = "#countryCode")
    public List<CityDto> getCities(String countryCode) {
        log.debug("Fetching cities for country: {}", countryCode);
        validateCountry(countryCode);
        
        return cityRepository.findByCountryCodeAndActiveTrue(countryCode.toUpperCase()).stream()
                .map(mapper::toDto)
                .toList();
    }

    public Page<CityDto> getCities(String countryCode, Pageable pageable) {
        log.debug("Fetching cities for country: {} (page {})", countryCode, pageable.pageNumber());
        validateCountry(countryCode);
        
        return cityRepository.findByCountryCodeAndActiveTrue(countryCode.toUpperCase(), pageable)
                .map(mapper::toDto);
    }

    @Cacheable(value = "city", key = "#id")
    public CityDto getCity(UUID id) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + id));
        return mapper.toDto(city);
    }

    // ===========================
    // Localities
    // ===========================

    @Cacheable(value = "localities", key = "#cityId")
    public List<LocalityDto> getLocalitiesByCity(UUID cityId) {
        log.debug("Fetching localities for city: {}", cityId);
        
        return localityRepository.findByCityIdAndActiveTrue(cityId).stream()
                .map(mapper::toDto)
                .toList();
    }

    public Page<LocalityDto> getLocalitiesByCity(UUID cityId, Pageable pageable) {
        log.debug("Fetching localities for city: {} (page {})", cityId, pageable.pageNumber());
        
        return localityRepository.findByCityIdAndActiveTrue(cityId, pageable)
                .map(mapper::toDto);
    }

    @Cacheable(value = "locality", key = "#id")
    public LocalityDto getLocality(UUID id) {
        Locality locality = localityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Locality not found: " + id));
        return mapper.toDto(locality);
    }

    // ===========================
    // Helper Methods
    // ===========================

    private void validateCountry(String countryCode) {
        if (!countryRepository.existsByCode(countryCode.toUpperCase())) {
            throw new ResourceNotFoundException("Country not found: " + countryCode);
        }
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
}
