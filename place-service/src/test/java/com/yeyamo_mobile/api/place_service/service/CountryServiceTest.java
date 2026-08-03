package com.yeyamo_mobile.api.place_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yeyamo_mobile.api.place_service.exception.ApiException;
import com.yeyamo_mobile.api.place_service.repository.CountryLanguageRepository;
import com.yeyamo_mobile.api.place_service.repository.CountryRepository;
import com.yeyamo_mobile.api.place_service.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CountryServiceTest {
    @Mock CountryRepository countryRepository;
    @Mock CountryLanguageRepository languageRepository;
    @Mock RegionRepository regionRepository;
    private CountryService service;

    @BeforeEach
    void setUp() {
        service = new CountryService(countryRepository, languageRepository, regionRepository);
    }

    @Test
    void rejectsInvalidCountryCodeBeforeDatabaseAccess() {
        ApiException error = assertThrows(ApiException.class, () -> service.get("invalid"));
        assertEquals("COUNTRY_CODE_INVALID", error.getCode());
        verifyNoInteractions(countryRepository, languageRepository, regionRepository);
    }
}
