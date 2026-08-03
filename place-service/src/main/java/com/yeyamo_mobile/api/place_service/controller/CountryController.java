package com.yeyamo_mobile.api.place_service.controller;

import com.yeyamo_mobile.api.place_service.dto.CountryLanguageResponse;
import com.yeyamo_mobile.api.place_service.dto.CountryResponse;
import com.yeyamo_mobile.api.place_service.dto.RegionResponse;
import com.yeyamo_mobile.api.place_service.service.CountryService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/countries")
public class CountryController {
    private final CountryService countryService;

    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    @GetMapping
    @Operation(summary = "Lister les pays disponibles", description = "Inclut le statut de lancement de chaque pays actif.")
    public List<CountryResponse> list() {
        return countryService.list();
    }

    @GetMapping("/{countryCode}")
    @Operation(summary = "Consulter un pays")
    public CountryResponse get(@PathVariable String countryCode) {
        return countryService.get(countryCode);
    }

    @GetMapping("/{countryCode}/administrative-areas")
    @Operation(summary = "Lister les zones administratives de niveau 1 d’un pays")
    public List<RegionResponse> administrativeAreas(@PathVariable String countryCode) {
        return countryService.administrativeAreas(countryCode);
    }

    @GetMapping("/{countryCode}/languages")
    @Operation(summary = "Lister les langues référencées pour un pays")
    public List<CountryLanguageResponse> languages(@PathVariable String countryCode) {
        return countryService.languages(countryCode);
    }
}
