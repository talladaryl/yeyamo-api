package com.yeyamo_mobile.api.place_service.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.place_service.dto.CityRequest;
import com.yeyamo_mobile.api.place_service.dto.CityResponse;
import com.yeyamo_mobile.api.place_service.exception.ApiException;
import com.yeyamo_mobile.api.place_service.models.City;
import com.yeyamo_mobile.api.place_service.models.Region;
import com.yeyamo_mobile.api.place_service.repository.CityRepository;
import com.yeyamo_mobile.api.place_service.util.SlugUtil;

@Service
@Transactional
public class CityService {

    private final CityRepository cityRepository;
    private final RegionService regionService;
    private final com.yeyamo_mobile.api.place_service.repository.DistrictRepository districtRepository;
    private final com.yeyamo_mobile.api.place_service.repository.PlaceRepository placeRepository;

    public CityService(CityRepository cityRepository, RegionService regionService,com.yeyamo_mobile.api.place_service.repository.DistrictRepository districtRepository,com.yeyamo_mobile.api.place_service.repository.PlaceRepository placeRepository) {
        this.cityRepository = cityRepository;
        this.regionService = regionService;
        this.districtRepository=districtRepository;this.placeRepository=placeRepository;
    }
    public CityResponse setActive(Long id,boolean active){City city=getEntityById(id);city.setActive(active);return CityResponse.from(cityRepository.save(city));}
    public void delete(Long id){City city=getEntityById(id);if(districtRepository.existsByCityId(id)||placeRepository.existsByCityId(id))throw new ApiException("CITY_IN_USE","La ville est referencee et ne peut pas etre supprimee",HttpStatus.CONFLICT);cityRepository.delete(city);}

    @Transactional(readOnly = true)
    public List<CityResponse> listByRegion(Long regionId) {
        return cityRepository.findByRegionId(regionId).stream().map(CityResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public City getEntityById(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new ApiException("CITY_NOT_FOUND", "Ville introuvable", HttpStatus.NOT_FOUND));
    }

    public CityResponse create(CityRequest request) {
        Region region = regionService.getEntityById(request.getRegionId());
        String slug = resolveSlug(request.getSlug(), request.getName());
        if (cityRepository.existsByRegionIdAndSlug(region.getId(), slug)) {
            throw new ApiException("CITY_SLUG_EXISTS", "Ce slug de ville existe deja dans cette region", HttpStatus.CONFLICT);
        }

        City city = new City();
        city.setRegion(region);
        city.setName(request.getName());
        city.setSlug(slug);
        city.setLatitude(request.getLatitude());
        city.setLongitude(request.getLongitude());
        return CityResponse.from(cityRepository.save(city));
    }

    public CityResponse update(Long id, CityRequest request) {
        City city = getEntityById(id);
        Region region = regionService.getEntityById(request.getRegionId());
        String slug = resolveSlug(request.getSlug(), request.getName());
        if (!city.getSlug().equals(slug) && cityRepository.existsByRegionIdAndSlug(region.getId(), slug)) {
            throw new ApiException("CITY_SLUG_EXISTS", "Ce slug de ville existe deja dans cette region", HttpStatus.CONFLICT);
        }

        city.setRegion(region);
        city.setName(request.getName());
        city.setSlug(slug);
        city.setLatitude(request.getLatitude());
        city.setLongitude(request.getLongitude());
        return CityResponse.from(cityRepository.save(city));
    }

    private String resolveSlug(String slug, String name) {
        if (slug != null && !slug.isBlank()) {
            return slug;
        }
        return SlugUtil.slugify(name);
    }
}
