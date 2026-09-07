package com.yeyamo_mobile.api.place_service.service;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import com.yeyamo_mobile.api.place_service.dto.PlaceRequest;
import com.yeyamo_mobile.api.place_service.dto.PlaceResponse;
import com.yeyamo_mobile.api.place_service.dto.PlaceSummaryResponse;
import com.yeyamo_mobile.api.place_service.dto.AdminPlaceResponse;
import com.yeyamo_mobile.api.place_service.dto.PartnerPlaceResponse;
import com.yeyamo_mobile.api.place_service.dto.PlaceStatusRequest;
import com.yeyamo_mobile.api.place_service.enums.PlaceStatus;
import com.yeyamo_mobile.api.place_service.event.PlaceEventPublisher;
import com.yeyamo_mobile.api.place_service.exception.ApiException;
import com.yeyamo_mobile.api.place_service.models.City;
import com.yeyamo_mobile.api.place_service.models.District;
import com.yeyamo_mobile.api.place_service.models.Place;
import com.yeyamo_mobile.api.place_service.models.PlaceCategory;
import com.yeyamo_mobile.api.place_service.models.PlaceMedia;
import com.yeyamo_mobile.api.place_service.models.PlaceSchedule;
import com.yeyamo_mobile.api.place_service.models.Region;
import com.yeyamo_mobile.api.place_service.models.PartnerReadModel;
import com.yeyamo_mobile.api.place_service.repository.PlaceRepository;
import com.yeyamo_mobile.api.place_service.repository.PartnerReadModelRepository;
import com.yeyamo_mobile.api.place_service.util.SlugUtil;

@Service
@Transactional
public class PlaceService {

    private static final int DEFAULT_NEARBY_LIMIT = 50;

    private final PlaceRepository placeRepository;
    private final RegionService regionService;
    private final CityService cityService;
    private final DistrictService districtService;
    private final CategoryService categoryService;
    private final PlaceEventPublisher eventPublisher;
    private final PartnerReadModelRepository partners;

    public PlaceService(
            PlaceRepository placeRepository,
            RegionService regionService,
            CityService cityService,
            DistrictService districtService,
            CategoryService categoryService,
            PlaceEventPublisher eventPublisher
    ) {
        this(placeRepository, regionService, cityService, districtService, categoryService, eventPublisher, null);
    }

    @Autowired
    public PlaceService(
            PlaceRepository placeRepository,
            RegionService regionService,
            CityService cityService,
            DistrictService districtService,
            CategoryService categoryService,
            PlaceEventPublisher eventPublisher,
            PartnerReadModelRepository partners
    ) {
        this.placeRepository = placeRepository;
        this.regionService = regionService;
        this.cityService = cityService;
        this.districtService = districtService;
        this.categoryService = categoryService;
        this.eventPublisher = eventPublisher;
        this.partners = partners;
    }

    @Transactional(readOnly = true)
    public PlaceResponse getById(UUID id) {
        Place place = placeRepository.findDetailedById(id)
                .orElseThrow(() -> new ApiException("PLACE_NOT_FOUND", "Lieu introuvable", HttpStatus.NOT_FOUND));
        return PlaceResponse.from(place);
    }

    @Transactional(readOnly = true)
    public List<PlaceSummaryResponse> findByRegionSlug(String slug) {
        regionService.getEntityBySlug(slug);
        return placeRepository.findByRegionSlug(slug, PlaceStatus.PUBLISHED).stream()
                .map(PlaceSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlaceSummaryResponse> findByCityId(UUID cityId) {
        cityService.getEntityById(cityId);
        return placeRepository.findByCityId(cityId, PlaceStatus.PUBLISHED).stream()
                .map(PlaceSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlaceSummaryResponse> findNearby(
            double latitude,
            double longitude,
            double radiusKm,
            Long categoryId,
            Integer limit
    ) {
        int effectiveLimit = limit == null || limit <= 0 ? DEFAULT_NEARBY_LIMIT : Math.min(limit, 100);
        double radiusMeters = radiusKm * 1000;
        return placeRepository.findNearby(latitude, longitude, radiusMeters, categoryId, effectiveLimit).stream()
                .map(PlaceSummaryResponse::from)
                .toList();
    }

    public PlaceResponse create(PlaceRequest request) {
        return create(request, null);
    }

    public PlaceResponse create(PlaceRequest request, String actorId) {
        if (actorId != null) {
            if (partners == null) {
                throw new ApiException("PARTNER_VALIDATION_UNAVAILABLE", "Validation partenaire indisponible", HttpStatus.SERVICE_UNAVAILABLE);
            }
            PartnerReadModel partner = partners.findById(request.getPartnerId())
                    .orElseThrow(() -> new ApiException("PARTNER_NOT_FOUND", "Partenaire introuvable", HttpStatus.NOT_FOUND));
            if (!partner.getOwnerUserId().equals(actorId)) {
                throw new ApiException("PARTNER_FORBIDDEN", "Ce partenaire ne vous appartient pas", HttpStatus.FORBIDDEN);
            }
            if (!partner.isApproved()) {
                throw new ApiException("PARTNER_NOT_APPROVED", "Le partenaire doit etre approuve avant de creer un lieu", HttpStatus.CONFLICT);
            }
        }
        Place place = new Place();
        applyRequest(place, request);
        String slug = resolveSlug(request.getSlug(), request.getName());
        if (placeRepository.existsBySlug(slug)) {
            throw new ApiException("PLACE_SLUG_EXISTS", "Ce slug de lieu existe deja", HttpStatus.CONFLICT);
        }
        place.setSlug(slug);
        Place saved = placeRepository.save(place);
        eventPublisher.publishCreated(saved);
        return PlaceResponse.from(placeRepository.findDetailedById(saved.getId()).orElse(saved));
    }

    public PlaceResponse update(UUID id, PlaceRequest request) {
        Place place = placeRepository.findDetailedById(id)
                .orElseThrow(() -> new ApiException("PLACE_NOT_FOUND", "Lieu introuvable", HttpStatus.NOT_FOUND));

        String slug = resolveSlug(request.getSlug(), request.getName());
        if (!place.getSlug().equals(slug) && placeRepository.existsBySlug(slug)) {
            throw new ApiException("PLACE_SLUG_EXISTS", "Ce slug de lieu existe deja", HttpStatus.CONFLICT);
        }

        applyRequest(place, request);
        place.setSlug(slug);
        Place saved = placeRepository.save(place);
        eventPublisher.publishUpdated(saved);
        return PlaceResponse.from(placeRepository.findDetailedById(saved.getId()).orElse(saved));
    }

    @Transactional(readOnly = true)
    public Page<AdminPlaceResponse> adminSearch(String search, PlaceStatus status, Long categoryId, Long regionId,
            UUID cityId, Long districtId, UUID partnerId, Boolean verified, Instant createdFrom, Instant createdTo,
            Pageable pageable) {
        Specification<Place> specification = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isBlank()) specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + search.trim().toLowerCase() + "%"));
        if (status != null) specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (categoryId != null) specification = specification.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        if (regionId != null) specification = specification.and((root, query, cb) -> cb.equal(root.get("region").get("id"), regionId));
        if (cityId != null) specification = specification.and((root, query, cb) -> cb.equal(root.get("city").get("id"), cityId));
        if (districtId != null) specification = specification.and((root, query, cb) -> cb.equal(root.get("district").get("id"), districtId));
        if (partnerId != null) specification = specification.and((root, query, cb) -> cb.equal(root.get("partnerId"), partnerId));
        if (verified != null) specification = specification.and((root, query, cb) -> cb.equal(root.get("verified"), verified));
        if (createdFrom != null) specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        if (createdTo != null) specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        return placeRepository.findAll(specification, pageable).map(AdminPlaceResponse::from);
    }

    /**
     * Returns only the authenticated partner's published places. Published is deliberate:
     * event-service accepts only active (published) place read-model entries for events.
     */
    @Transactional(readOnly = true)
    public Page<PartnerPlaceResponse> findMyPublishedPlaces(String actorId, Pageable pageable) {
        if (partners == null) {
            throw new ApiException("PARTNER_VALIDATION_UNAVAILABLE", "Validation partenaire indisponible", HttpStatus.SERVICE_UNAVAILABLE);
        }

        PartnerReadModel partner = partners.findByOwnerUserId(actorId)
                .orElseThrow(() -> new ApiException("PARTNER_NOT_FOUND", "Partenaire introuvable", HttpStatus.NOT_FOUND));
        if (!partner.isApproved()) {
            throw new ApiException("PARTNER_NOT_APPROVED", "Le partenaire doit etre approuve", HttpStatus.CONFLICT);
        }

        return adminSearch(null, PlaceStatus.PUBLISHED, null, null, null, null,
                partner.getPartnerId(), null, null, null, pageable)
                .map(PartnerPlaceResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminPlaceResponse adminDetail(UUID id) {
        return AdminPlaceResponse.from(placeRepository.findDetailedById(id)
                .orElseThrow(() -> new ApiException("PLACE_NOT_FOUND", "Lieu introuvable", HttpStatus.NOT_FOUND)));
    }

    public AdminPlaceResponse updateStatus(UUID id, PlaceStatusRequest request) {
        Place place = placeRepository.findDetailedById(id)
                .orElseThrow(() -> new ApiException("PLACE_NOT_FOUND", "Lieu introuvable", HttpStatus.NOT_FOUND));
        place.setStatus(request.status());
        place.setVerified(request.verified());
        Place saved = placeRepository.save(place);
        eventPublisher.publishUpdated(saved);
        return AdminPlaceResponse.from(saved);
    }

    private void applyRequest(Place place, PlaceRequest request) {
        Region region = regionService.getEntityById(request.getRegionId());
        City city = cityService.getEntityById(request.getCityId());
        if (!city.isActive()) {
            throw new ApiException("CITY_INACTIVE", "La ville selectionnee n'est pas active", HttpStatus.BAD_REQUEST);
        }
        if (!city.getRegion().getId().equals(region.getId())) {
            throw new ApiException("CITY_REGION_MISMATCH", "La ville n'appartient pas a cette region", HttpStatus.BAD_REQUEST);
        }

        District district = null;
        if (request.getDistrictId() != null) {
            district = districtService.getEntityById(request.getDistrictId());
            if (!district.getCity().getId().equals(city.getId())) {
                throw new ApiException("DISTRICT_CITY_MISMATCH", "Le quartier n'appartient pas a cette ville", HttpStatus.BAD_REQUEST);
            }
        }

        PlaceCategory category = categoryService.getById(request.getCategoryId());

        place.setPartnerId(request.getPartnerId());
        place.setCategory(category);
        place.setRegion(region);
        place.setCity(city);
        place.setDistrict(district);
        place.setName(request.getName());
        place.setDescription(request.getDescription());
        place.setLatitude(request.getLatitude());
        place.setLongitude(request.getLongitude());
        place.setAddress(request.getAddress());
        place.setPhone(request.getPhone());
        place.setWebsite(request.getWebsite());
        if (request.getStatus() != null) {
            place.setStatus(request.getStatus());
        }

        place.getMedia().clear();
        if (request.getMedia() != null) {
            for (PlaceRequest.PlaceMediaRequest mediaRequest : request.getMedia()) {
                PlaceMedia media = new PlaceMedia();
                media.setPlace(place);
                media.setUrl(mediaRequest.getUrl());
                media.setType(mediaRequest.getType());
                media.setDisplayOrder(mediaRequest.getDisplayOrder() != null ? mediaRequest.getDisplayOrder() : 0);
                place.getMedia().add(media);
            }
        }

        place.getSchedules().clear();
        if (request.getSchedules() != null) {
            for (PlaceRequest.PlaceScheduleRequest scheduleRequest : request.getSchedules()) {
                PlaceSchedule schedule = new PlaceSchedule();
                schedule.setPlace(place);
                schedule.setDayOfWeek(scheduleRequest.getDayOfWeek());
                schedule.setOpenTime(scheduleRequest.getOpenTime());
                schedule.setCloseTime(scheduleRequest.getCloseTime());
                place.getSchedules().add(schedule);
            }
        }
    }

    private String resolveSlug(String slug, String name) {
        if (slug != null && !slug.isBlank()) {
            return slug;
        }
        return SlugUtil.slugify(name);
    }
}
