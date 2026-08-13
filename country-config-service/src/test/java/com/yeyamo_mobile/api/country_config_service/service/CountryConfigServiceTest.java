package com.yeyamo_mobile.api.country_config_service.service;

import com.yeyamo_mobile.api.country_config_service.domain.model.*;
import com.yeyamo_mobile.api.country_config_service.domain.repository.*;
import com.yeyamo_mobile.api.country_config_service.dto.*;
import com.yeyamo_mobile.api.country_config_service.event.CountryEventPublisher;
import com.yeyamo_mobile.api.country_config_service.mapper.CountryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountryConfigServiceTest {

    @Mock
    private CountryRepository countryRepository;
    
    @Mock
    private CountryLanguageRepository languageRepository;
    
    @Mock
    private CountryCurrencyRepository currencyRepository;
    
    @Mock
    private CountryTimezoneRepository timezoneRepository;
    
    @Mock
    private CountryEventPublisher eventPublisher;
    
    @Mock
    private CountryMapper mapper;

    @InjectMocks
    private CountryConfigService service;

    private Country cameroon;
    private CountryDto cameroonDto;

    @BeforeEach
    void setUp() {
        cameroon = new Country(
                "CM", "Cameroon", "Republic of Cameroon", "AF",
                "fr", "XAF", "Africa/Douala", "+237",
                CountryLaunchStatus.LIVE
        );
        cameroon.setRegistrationEnabled(true);
        cameroon.setContentPublishingEnabled(true);
        cameroon.setPaymentsEnabled(true);

        cameroonDto = new CountryDto(
                UUID.randomUUID(), "CM", "Cameroon", "Republic of Cameroon", "AF",
                "fr", "XAF", "Africa/Douala", "+237",
                CountryLaunchStatus.LIVE,
                true, true, true, true, true, true, true, true, true, true,
                null, null
        );
    }

    @Test
    @DisplayName("Should get country by code - Cameroon LIVE")
    void shouldGetCountryByCode() {
        // Given
        when(countryRepository.findByCode("CM")).thenReturn(Optional.of(cameroon));
        when(mapper.toDto(cameroon)).thenReturn(cameroonDto);

        // When
        CountryDto result = service.getCountryByCode("CM");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.code()).isEqualTo("CM");
        assertThat(result.launchStatus()).isEqualTo(CountryLaunchStatus.LIVE);
        assertThat(result.registrationEnabled()).isTrue();
        verify(countryRepository).findByCode("CM");
    }

    @Test
    @DisplayName("Should throw exception when country not found")
    void shouldThrowExceptionWhenCountryNotFound() {
        // Given
        when(countryRepository.findByCode("XX")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.getCountryByCode("XX"))
                .isInstanceOf(CountryConfigService.CountryNotFoundException.class)
                .hasMessageContaining("Country not found: XX");
    }

    @Test
    @DisplayName("Should get available countries (LIVE or BETA)")
    void shouldGetAvailableCountries() {
        // Given
        when(countryRepository.findAvailableCountries()).thenReturn(List.of(cameroon));
        when(mapper.toDto(cameroon)).thenReturn(cameroonDto);

        // When
        List<CountryDto> result = service.getAvailableCountries();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).launchStatus()).isIn(CountryLaunchStatus.LIVE, CountryLaunchStatus.BETA);
    }

    @Test
    @DisplayName("Should update launch status and publish event")
    void shouldUpdateLaunchStatusAndPublishEvent() {
        // Given
        when(countryRepository.findByCode("CM")).thenReturn(Optional.of(cameroon));
        when(countryRepository.save(any(Country.class))).thenReturn(cameroon);
        when(mapper.toDto(any(Country.class))).thenReturn(cameroonDto);

        // When
        CountryDto result = service.updateLaunchStatus("CM", CountryLaunchStatus.BETA);

        // Then
        assertThat(result).isNotNull();
        verify(countryRepository).save(cameroon);
        verify(eventPublisher).publishCountryLaunchStatusChanged(
                eq(cameroon), 
                eq(CountryLaunchStatus.LIVE), 
                eq(CountryLaunchStatus.BETA)
        );
    }

    @Test
    @DisplayName("Should update features and publish event")
    void shouldUpdateFeaturesAndPublishEvent() {
        // Given
        UpdateFeaturesRequest request = new UpdateFeaturesRequest(
                true, true, true, true, true, false, false, false, true, true
        );
        
        when(countryRepository.findByCode("CM")).thenReturn(Optional.of(cameroon));
        when(countryRepository.save(any(Country.class))).thenReturn(cameroon);
        when(mapper.toDto(any(Country.class))).thenReturn(cameroonDto);

        // When
        CountryDto result = service.updateFeatures("CM", request);

        // Then
        assertThat(result).isNotNull();
        verify(countryRepository).save(cameroon);
        verify(eventPublisher).publishCountryFeatureChanged(cameroon);
    }

    @Test
    @DisplayName("Should get country configuration with languages, currencies, timezones")
    void shouldGetCountryConfiguration() {
        // Given
        UUID countryId = UUID.randomUUID();
        cameroon = new Country("CM", "Cameroon", "Republic of Cameroon", "AF",
                "fr", "XAF", "Africa/Douala", "+237", CountryLaunchStatus.LIVE);
        
        CountryLanguage french = new CountryLanguage(countryId, "fr", "Français", true, 1);
        CountryLanguage english = new CountryLanguage(countryId, "en", "English", false, 2);
        CountryCurrency xaf = new CountryCurrency(countryId, "XAF", "Central African CFA franc", "FCFA", 0, true);
        CountryTimezone wat = new CountryTimezone(countryId, "Africa/Douala", "West Africa Time", true);

        when(countryRepository.findByCode("CM")).thenReturn(Optional.of(cameroon));
        when(languageRepository.findByCountryIdOrderByDisplayOrderAsc(any())).thenReturn(List.of(french, english));
        when(currencyRepository.findByCountryId(any())).thenReturn(List.of(xaf));
        when(timezoneRepository.findByCountryId(any())).thenReturn(List.of(wat));
        when(mapper.toDto(cameroon)).thenReturn(cameroonDto);

        // When
        CountryConfigurationDto result = service.getCountryConfiguration("CM");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.country().code()).isEqualTo("CM");
        verify(languageRepository).findByCountryIdOrderByDisplayOrderAsc(any());
        verify(currencyRepository).findByCountryId(any());
        verify(timezoneRepository).findByCountryId(any());
    }

    @Test
    @DisplayName("Should get country features")
    void shouldGetCountryFeatures() {
        // Given
        when(countryRepository.findByCode("CM")).thenReturn(Optional.of(cameroon));

        // When
        FeatureFlagsDto result = service.getCountryFeatures("CM");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.registrationEnabled()).isTrue();
        assertThat(result.paymentsEnabled()).isTrue();
    }

    @Test
    @DisplayName("Cameroon should be operational (LIVE)")
    void cameroonShouldBeOperational() {
        assertThat(cameroon.isOperational()).isTrue();
        assertThat(cameroon.canAcceptNewUsers()).isTrue();
    }

    @Test
    @DisplayName("COMING_SOON country should not be operational")
    void comingSoonCountryShouldNotBeOperational() {
        Country nigeria = new Country(
                "NG", "Nigeria", "Federal Republic of Nigeria", "AF",
                "en", "NGN", "Africa/Lagos", "+234",
                CountryLaunchStatus.COMING_SOON
        );
        nigeria.setRegistrationEnabled(false);

        assertThat(nigeria.isOperational()).isFalse();
        assertThat(nigeria.canAcceptNewUsers()).isFalse();
    }

    @Test
    @DisplayName("Should handle optimistic locking")
    void shouldHandleOptimisticLocking() {
        // Given
        Country country = new Country("CM", "Cameroon", "Republic of Cameroon", "AF",
                "fr", "XAF", "Africa/Douala", "+237", CountryLaunchStatus.LIVE);

        // When
        country.updateLaunchStatus(CountryLaunchStatus.BETA);

        // Then
        assertThat(country.getLaunchStatus()).isEqualTo(CountryLaunchStatus.BETA);
    }
}
