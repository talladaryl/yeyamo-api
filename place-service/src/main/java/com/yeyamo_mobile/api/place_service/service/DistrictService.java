package com.yeyamo_mobile.api.place_service.service;

import java.util.UUID;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.place_service.dto.DistrictRequest;
import com.yeyamo_mobile.api.place_service.dto.DistrictResponse;
import com.yeyamo_mobile.api.place_service.exception.ApiException;
import com.yeyamo_mobile.api.place_service.models.City;
import com.yeyamo_mobile.api.place_service.models.District;
import com.yeyamo_mobile.api.place_service.repository.DistrictRepository;

@Service
@Transactional
public class DistrictService {

    private final DistrictRepository districtRepository;
    private final CityService cityService;
    private final com.yeyamo_mobile.api.place_service.repository.PlaceRepository placeRepository;

    public DistrictService(DistrictRepository districtRepository, CityService cityService,com.yeyamo_mobile.api.place_service.repository.PlaceRepository placeRepository) {
        this.districtRepository = districtRepository;
        this.cityService = cityService;
        this.placeRepository=placeRepository;
    }

    @Transactional(readOnly = true)
    public List<DistrictResponse> listByCity(UUID cityId) {
        return districtRepository.findByCityId(cityId).stream().map(DistrictResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public District getEntityById(Long id) {
        return districtRepository.findById(id)
                .orElseThrow(() -> new ApiException("DISTRICT_NOT_FOUND", "Quartier introuvable", HttpStatus.NOT_FOUND));
    }

    public DistrictResponse create(DistrictRequest request) {
        City city = cityService.getEntityById(request.getCityId());
        if(districtRepository.existsByCityIdAndNameIgnoreCase(city.getId(),request.getName()))throw new ApiException("DISTRICT_EXISTS","Ce quartier existe deja dans cette ville",HttpStatus.CONFLICT);
        District district = new District();
        district.setCity(city);
        district.setName(request.getName());
        district.setLatitude(request.getLatitude());
        district.setLongitude(request.getLongitude());
        return DistrictResponse.from(districtRepository.save(district));
    }

    public DistrictResponse update(Long id, DistrictRequest request) {
        District district = getEntityById(id);
        City city = cityService.getEntityById(request.getCityId());
        district.setCity(city);
        district.setName(request.getName());
        district.setLatitude(request.getLatitude());
        district.setLongitude(request.getLongitude());
        return DistrictResponse.from(districtRepository.save(district));
    }

    public DistrictResponse setActive(Long id,boolean active){District district=getEntityById(id);district.setActive(active);return DistrictResponse.from(districtRepository.save(district));}
    public void delete(Long id){District district=getEntityById(id);if(placeRepository.existsByDistrictId(id))throw new ApiException("DISTRICT_IN_USE","Le quartier est reference et ne peut pas etre supprime",HttpStatus.CONFLICT);districtRepository.delete(district);}
}
