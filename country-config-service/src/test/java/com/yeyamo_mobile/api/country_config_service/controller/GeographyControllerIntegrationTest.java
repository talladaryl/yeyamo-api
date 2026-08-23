package com.yeyamo_mobile.api.country_config_service.controller;

import com.yeyamo_mobile.api.country_config_service.domain.model.*;
import com.yeyamo_mobile.api.country_config_service.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GeographyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private AdministrativeAreaRepository adminAreaRepository;

    @Autowired
    private AdministrativeLevelLabelRepository levelLabelRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private LocalityRepository localityRepository;

    private Country cameroon;
    private AdministrativeArea regionLittoral;
    private AdministrativeArea departmentWouri;
    private City cityDouala;

    @BeforeEach
    void setUp() {
        // Clean up
        localityRepository.deleteAll();
        cityRepository.deleteAll();
        adminAreaRepository.deleteAll();
        levelLabelRepository.deleteAll();
        countryRepository.deleteAll();

        // Create Cameroon
        cameroon = new Country(
                "CM", "Cameroon", "Republic of Cameroon", "AF",
                "fr", "XAF", "Africa/Douala", "+237",
                CountryLaunchStatus.LIVE
        );
        countryRepository.save(cameroon);

        // Create level labels for Cameroon
        AdministrativeLevelLabel label1 = new AdministrativeLevelLabel("CM", 1, "Région", "Régions", 1);
        AdministrativeLevelLabel label2 = new AdministrativeLevelLabel("CM", 2, "Département", "Départements", 2);
        levelLabelRepository.save(label1);
        levelLabelRepository.save(label2);

        // Create administrative hierarchy
        regionLittoral = new AdministrativeArea("CM", null, 1, "Littoral", "littoral");
        regionLittoral.setLatitude(new BigDecimal("4.05"));
        regionLittoral.setLongitude(new BigDecimal("9.7"));
        adminAreaRepository.save(regionLittoral);

        departmentWouri = new AdministrativeArea("CM", regionLittoral.getId(), 2, "Wouri", "wouri");
        departmentWouri.setLatitude(new BigDecimal("4.05"));
        departmentWouri.setLongitude(new BigDecimal("9.7"));
        adminAreaRepository.save(departmentWouri);

        // Create city
        cityDouala = new City("CM", departmentWouri.getId(), "Douala", "douala");
        cityDouala.setLatitude(new BigDecimal("4.0511"));
        cityDouala.setLongitude(new BigDecimal("9.7679"));
        cityDouala.setPopulation(3000000L);
        cityRepository.save(cityDouala);

        // Create localities (quartiers)
        Locality akwa = new Locality("CM", cityDouala.getId(), null, "quartier", "Akwa", "akwa");
        Locality bonanjo = new Locality("CM", cityDouala.getId(), null, "quartier", "Bonanjo", "bonanjo");
        localityRepository.save(akwa);
        localityRepository.save(bonanjo);
    }

    @Test
    @DisplayName("GET /api/v1/countries/CM/administrative-areas - Should return all areas")
    void shouldReturnAllAdministrativeAreas() throws Exception {
        mockMvc.perform(get("/api/v1/countries/CM/administrative-areas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Littoral", "Wouri")));
    }

    @Test
    @DisplayName("GET /api/v1/countries/CM/administrative-areas/top-level - Should return only regions")
    void shouldReturnTopLevelAreasOnly() throws Exception {
        mockMvc.perform(get("/api/v1/countries/CM/administrative-areas/top-level")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Littoral")))
                .andExpect(jsonPath("$[0].level", is(1)))
                .andExpect(jsonPath("$[0].parentId").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/administrative-areas/{id}/children - Should return departments")
    void shouldReturnChildrenOfRegion() throws Exception {
        mockMvc.perform(get("/api/v1/administrative-areas/" + regionLittoral.getId() + "/children")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Wouri")))
                .andExpect(jsonPath("$[0].level", is(2)))
                .andExpect(jsonPath("$[0].parentId", is(regionLittoral.getId().toString())));
    }

    @Test
    @DisplayName("GET /api/v1/countries/CM/administrative-labels - Should return Cameroon labels")
    void shouldReturnAdministrativeLabels() throws Exception {
        mockMvc.perform(get("/api/v1/countries/CM/administrative-labels")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].level", is(1)))
                .andExpect(jsonPath("$[0].label", is("Région")))
                .andExpect(jsonPath("$[1].level", is(2)))
                .andExpect(jsonPath("$[1].label", is("Département")));
    }

    @Test
    @DisplayName("GET /api/v1/countries/CM/cities - Should return Douala")
    void shouldReturnCities() throws Exception {
        mockMvc.perform(get("/api/v1/countries/CM/cities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Douala")))
                .andExpect(jsonPath("$[0].population", is(3000000)))
                .andExpect(jsonPath("$[0].administrativeAreaId", is(departmentWouri.getId().toString())));
    }

    @Test
    @DisplayName("GET /api/v1/cities/{id}/localities - Should return quartiers")
    void shouldReturnLocalitiesByCity() throws Exception {
        mockMvc.perform(get("/api/v1/cities/" + cityDouala.getId() + "/localities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Akwa", "Bonanjo")))
                .andExpect(jsonPath("$[0].localityType", is("quartier")))
                .andExpect(jsonPath("$[0].cityId", is(cityDouala.getId().toString())));
    }

    @Test
    @DisplayName("GET /api/v1/countries/XX/administrative-areas - Should return 404")
    void shouldReturn404ForUnknownCountry() throws Exception {
        mockMvc.perform(get("/api/v1/countries/XX/administrative-areas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }

    @Test
    @DisplayName("Hierarchy consistency - Parent country must match")
    void shouldMaintainHierarchyConsistency() throws Exception {
        mockMvc.perform(get("/api/v1/administrative-areas/" + departmentWouri.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode", is("CM")))
                .andExpect(jsonPath("$.level", is(2)))
                .andExpect(jsonPath("$.parentId", is(regionLittoral.getId().toString())));
    }
}
