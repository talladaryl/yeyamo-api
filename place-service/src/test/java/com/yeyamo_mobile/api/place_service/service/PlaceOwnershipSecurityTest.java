package com.yeyamo_mobile.api.place_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.place_service.dto.PlaceRequest;
import com.yeyamo_mobile.api.place_service.enums.PlaceStatus;
import com.yeyamo_mobile.api.place_service.event.PlaceEventPublisher;
import com.yeyamo_mobile.api.place_service.exception.ApiException;
import com.yeyamo_mobile.api.place_service.models.*;
import com.yeyamo_mobile.api.place_service.repository.*;

class PlaceOwnershipSecurityTest {
    private final PlaceRepository places=mock(PlaceRepository.class);private final PartnerReadModelRepository partners=mock(PartnerReadModelRepository.class);
    private final RegionService regions=mock(RegionService.class);private final CityService cities=mock(CityService.class);private final CategoryService categories=mock(CategoryService.class);
    private final PlaceService service=new PlaceService(places,regions,cities,mock(DistrictService.class),categories,mock(PlaceEventPublisher.class),partners);
    private UUID partnerId;private Place place;private PlaceRequest request;
    @BeforeEach void setup(){partnerId=UUID.randomUUID();Region region=new Region();region.setId(1L);region.setName("Centre");region.setCountryCode("CM");City city=new City();city.setId(UUID.randomUUID());city.setRegion(region);city.setName("Yaounde");city.setActive(true);PlaceCategory category=new PlaceCategory();category.setId(2L);category.setName("Culture");place=new Place();place.setId(UUID.randomUUID());place.setPartnerId(partnerId);place.setRegion(region);place.setCity(city);place.setCategory(category);place.setName("Ancien");place.setSlug("ancien");place.setLatitude(3.8);place.setLongitude(11.5);place.setStatus(PlaceStatus.PUBLISHED);request=new PlaceRequest();request.setPartnerId(partnerId);request.setRegionId(1L);request.setCityId(city.getId());request.setCategoryId(2L);request.setName("Nouveau");request.setLatitude(3.9);request.setLongitude(11.6);request.setStatus(PlaceStatus.PUBLISHED);when(places.findDetailedById(place.getId())).thenReturn(Optional.of(place));when(regions.getEntityById(1L)).thenReturn(region);when(cities.getEntityById(city.getId())).thenReturn(city);when(categories.getById(2L)).thenReturn(category);when(places.save(any())).thenAnswer(i->i.getArgument(0));}
    @Test void ownerOfApprovedPartnerCanUpdate(){when(partners.findByOwnerUserId("owner")).thenReturn(Optional.of(partner("owner","APPROVED",partnerId)));assertEquals("Nouveau",service.updateAuthorized(place.getId(),request,"owner",false).name());}
    @Test void anotherPartnerGetsNotFound(){when(partners.findByOwnerUserId("other")).thenReturn(Optional.of(partner("other","APPROVED",UUID.randomUUID())));ApiException e=assertThrows(ApiException.class,()->service.updateAuthorized(place.getId(),request,"other",false));assertEquals("PLACE_NOT_FOUND",e.getCode());verify(places,never()).save(any());}
    @Test void explorerGetsNotFound(){when(partners.findByOwnerUserId("explorer")).thenReturn(Optional.empty());ApiException e=assertThrows(ApiException.class,()->service.updateAuthorized(place.getId(),request,"explorer",false));assertEquals("PLACE_NOT_FOUND",e.getCode());}
    @Test void suspendedPartnerIsRejected(){when(partners.findByOwnerUserId("owner")).thenReturn(Optional.of(partner("owner","SUSPENDED",partnerId)));ApiException e=assertThrows(ApiException.class,()->service.updateAuthorized(place.getId(),request,"owner",false));assertEquals("PARTNER_NOT_APPROVED",e.getCode());}
    @Test void ownerCannotTransferPlace(){when(partners.findByOwnerUserId("owner")).thenReturn(Optional.of(partner("owner","APPROVED",partnerId)));request.setPartnerId(UUID.randomUUID());ApiException e=assertThrows(ApiException.class,()->service.updateAuthorized(place.getId(),request,"owner",false));assertEquals("PLACE_PROTECTED_FIELD",e.getCode());}
    @Test void ownerCannotPublishThroughUpdate(){when(partners.findByOwnerUserId("owner")).thenReturn(Optional.of(partner("owner","APPROVED",partnerId)));place.setStatus(PlaceStatus.DRAFT);request.setStatus(PlaceStatus.PUBLISHED);ApiException e=assertThrows(ApiException.class,()->service.updateAuthorized(place.getId(),request,"owner",false));assertEquals("PLACE_PROTECTED_FIELD",e.getCode());}
    @Test void adminCanUseExistingAdministrativeUpdate(){assertEquals("Nouveau",service.updateAuthorized(place.getId(),request,"admin",true).name());verifyNoInteractions(partners);}
    @Test void missingPlaceDoesNotRevealOwnership(){UUID missing=UUID.randomUUID();when(places.findDetailedById(missing)).thenReturn(Optional.empty());ApiException e=assertThrows(ApiException.class,()->service.updateAuthorized(missing,request,"owner",false));assertEquals("PLACE_NOT_FOUND",e.getCode());verifyNoInteractions(partners);}
    private PartnerReadModel partner(String owner,String status,UUID id){return new PartnerReadModel(id,owner,status,Instant.now());}
}
