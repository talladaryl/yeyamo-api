package com.yeyamo_mobile.api.place_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.RequestParam;

import com.yeyamo_mobile.api.place_service.controller.PlaceController;
import com.yeyamo_mobile.api.place_service.enums.PlaceStatus;
import com.yeyamo_mobile.api.place_service.event.PlaceEventPublisher;
import com.yeyamo_mobile.api.place_service.models.City;
import com.yeyamo_mobile.api.place_service.models.PartnerReadModel;
import com.yeyamo_mobile.api.place_service.models.Place;
import com.yeyamo_mobile.api.place_service.models.PlaceCategory;
import com.yeyamo_mobile.api.place_service.models.Region;
import com.yeyamo_mobile.api.place_service.repository.PartnerReadModelRepository;
import com.yeyamo_mobile.api.place_service.repository.PlaceRepository;

class PartnerPlaceListingContractTest {

    @Test
    void endpointDoesNotAcceptAPartnerIdRequestParameter() throws NoSuchMethodException {
        Method endpoint = PlaceController.class.getMethod("myPublishedPlaces",
                org.springframework.security.core.Authentication.class, Pageable.class);

        assertEquals(2, endpoint.getParameterCount());
        assertEquals(0, java.util.Arrays.stream(endpoint.getParameters())
                .filter(parameter -> parameter.isAnnotationPresent(RequestParam.class))
                .count());
    }

    @Test
    void partnerACanOnlyReceiveItsOwnPublishedPlacesAndCannotSelectPartnerB() {
        PlaceRepository places = mock(PlaceRepository.class);
        PartnerReadModelRepository partners = mock(PartnerReadModelRepository.class);
        UUID partnerA = UUID.randomUUID();
        UUID placeA = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        when(partners.findByOwnerUserId("partner-a-user"))
                .thenReturn(Optional.of(new PartnerReadModel(partnerA, "partner-a-user", "APPROVED", Instant.now())));
        when(places.findAll(org.mockito.ArgumentMatchers.<Specification<Place>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(publishedPlace(placeA, partnerA)), pageable, 1));

        PlaceService service = new PlaceService(
                places, mock(RegionService.class), mock(CityService.class), mock(DistrictService.class),
                mock(CategoryService.class), mock(PlaceEventPublisher.class), partners);

        var response = service.findMyPublishedPlaces("partner-a-user", pageable);

        assertEquals(List.of(placeA), response.getContent().stream().map(place -> place.id()).toList());
        assertEquals(List.of("PUBLISHED"), response.getContent().stream().map(place -> place.status()).toList());
        verify(partners).findByOwnerUserId("partner-a-user");
        verify(partners, never()).findByOwnerUserId("partner-b-user");
        verify(places).findAll(org.mockito.ArgumentMatchers.<Specification<Place>>any(), eq(pageable));
        // The service signature accepts only the JWT subject and Pageable: no partnerId can be overridden by a caller.
    }

    private Place publishedPlace(UUID placeId, UUID partnerId) {
        Region region = new Region();
        region.setId(1L);
        region.setName("Centre");
        PlaceCategory category = new PlaceCategory();
        category.setId(2L);
        category.setName("Culture");
        City city = new City();
        city.setId(UUID.randomUUID());
        city.setName("Yaounde");

        Place place = new Place();
        place.setId(placeId);
        place.setPartnerId(partnerId);
        place.setName("Lieu A");
        place.setSlug("lieu-a");
        place.setStatus(PlaceStatus.PUBLISHED);
        place.setRegion(region);
        place.setCategory(category);
        place.setCity(city);
        place.setLatitude(3.8);
        place.setLongitude(11.5);
        return place;
    }
}
