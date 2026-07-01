package com.yeyamo_mobile.api.place_service.service;

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

    public DistrictService(DistrictRepository districtRepository, CityService cityService) {
        this.districtRepository = districtRepository;
        this.cityService = cityService;
    }

    @Transactional(readOnly = true)
    public List<DistrictResponse> listByCity(Long cityId) {
        return districtRepository.findByCityId(cityId).stream().map(DistrictResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public District getEntityById(Long id) {
        return districtRepository.findById(id)
                .orElseThrow(() -> new ApiException("DISTRICT_NOT_FOUND", "Quartier introuvable", HttpStatus.NOT_FOUND));
    }

    public DistrictResponse create(DistrictRequest request) {
        City city = cityService.getEntityById(request.getCityId());
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
}
