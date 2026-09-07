package com.yeyamo_mobile.api.place_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;

import com.yeyamo_mobile.api.place_service.dto.PlaceRequest;
import com.yeyamo_mobile.api.place_service.enums.PlaceStatus;
import com.yeyamo_mobile.api.place_service.event.PlaceEventPublisher;
import com.yeyamo_mobile.api.place_service.exception.ApiException;
import com.yeyamo_mobile.api.place_service.models.City;
import com.yeyamo_mobile.api.place_service.models.Place;
import com.yeyamo_mobile.api.place_service.models.PlaceCategory;
import com.yeyamo_mobile.api.place_service.models.Region;
import com.yeyamo_mobile.api.place_service.repository.PlaceRepository;

class PlaceCreationContractTest {
    @Test
    void createsPlaceForAnExistingActiveCity() {
        PlaceRepository places = mock(PlaceRepository.class);
        RegionService regions = mock(RegionService.class);
        CityService cities = mock(CityService.class);
        CategoryService categories = mock(CategoryService.class);
        PlaceEventPublisher publisher = mock(PlaceEventPublisher.class);
        PlaceService service = new PlaceService(places, regions, cities, mock(DistrictService.class), categories, publisher);
        Region region = region();
        City city = city(region, true);
        PlaceCategory category = new PlaceCategory(); category.setId(7L); category.setName("Musee"); category.setSlug("musee");
        when(regions.getEntityById(region.getId())).thenReturn(region);
        when(cities.getEntityById(city.getId())).thenReturn(city);
        when(categories.getById(7L)).thenReturn(category);
        when(places.existsBySlug("musee-national")).thenReturn(false);
        when(places.save(any(Place.class))).thenAnswer(invocation -> {
            Place saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(places.findDetailedById(any())).thenReturn(Optional.empty());

        var response = service.create(request(city.getId(), region.getId()));

        assertEquals("Musee national", response.name());
        assertEquals(city.getId(), response.cityId());
    }

    @Test
    void rejectsAnInactiveCity() {
        PlaceRepository places = mock(PlaceRepository.class);
        RegionService regions = mock(RegionService.class);
        CityService cities = mock(CityService.class);
        Region region = region();
        City city = city(region, false);
        when(regions.getEntityById(region.getId())).thenReturn(region);
        when(cities.getEntityById(city.getId())).thenReturn(city);
        PlaceService service = new PlaceService(places, regions, cities, mock(DistrictService.class), mock(CategoryService.class), mock(PlaceEventPublisher.class));

        ApiException exception = assertThrows(ApiException.class, () -> service.create(request(city.getId(), region.getId())));

        assertEquals("CITY_INACTIVE", exception.getCode());
    }

    @Test
    void requiresTheMandatoryMobileFields() {
        var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(new PlaceRequest());

        assertEquals(6, violations.size());
    }

    private PlaceRequest request(UUID cityId, Long regionId) {
        PlaceRequest request = new PlaceRequest();
        request.setCategoryId(7L); request.setRegionId(regionId); request.setCityId(cityId);
        request.setName("Musee national"); request.setLatitude(3.8667); request.setLongitude(11.5167);
        request.setStatus(PlaceStatus.DRAFT);
        return request;
    }

    private Region region() {
        Region region = new Region(); region.setId(3L); region.setName("Centre"); region.setSlug("centre"); region.setCode("CE"); return region;
    }

    private City city(Region region, boolean active) {
        City city = new City(); city.setId(UUID.randomUUID()); city.setRegion(region); city.setName("Yaounde"); city.setSlug("yaounde"); city.setActive(active); return city;
    }
}
