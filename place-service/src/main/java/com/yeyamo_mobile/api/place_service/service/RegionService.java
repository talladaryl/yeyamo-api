package com.yeyamo_mobile.api.place_service.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.place_service.dto.RegionRequest;
import com.yeyamo_mobile.api.place_service.dto.RegionResponse;
import com.yeyamo_mobile.api.place_service.exception.ApiException;
import com.yeyamo_mobile.api.place_service.models.Region;
import com.yeyamo_mobile.api.place_service.repository.RegionRepository;
import com.yeyamo_mobile.api.place_service.util.SlugUtil;

@Service
@Transactional
public class RegionService {

    private final RegionRepository regionRepository;

    public RegionService(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    @Transactional(readOnly = true)
    public List<RegionResponse> listRegions() {
        return regionRepository.findAll().stream().map(RegionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RegionResponse getBySlug(String slug) {
        return RegionResponse.from(getEntityBySlug(slug));
    }

    @Transactional(readOnly = true)
    public Region getEntityBySlug(String slug) {
        return regionRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException("REGION_NOT_FOUND", "Region introuvable", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Region getEntityById(Long id) {
        return regionRepository.findById(id)
                .orElseThrow(() -> new ApiException("REGION_NOT_FOUND", "Region introuvable", HttpStatus.NOT_FOUND));
    }

    public RegionResponse create(RegionRequest request) {
        String slug = resolveSlug(request.getSlug(), request.getName());
        if (regionRepository.existsBySlug(slug)) {
            throw new ApiException("REGION_SLUG_EXISTS", "Ce slug de region existe deja", HttpStatus.CONFLICT);
        }

        Region region = new Region();
        region.setName(request.getName());
        region.setSlug(slug);
        region.setCode(request.getCode());
        region.setDescription(request.getDescription());
        region.setCoverImage(request.getCoverImage());
        return RegionResponse.from(regionRepository.save(region));
    }

    public RegionResponse update(Long id, RegionRequest request) {
        Region region = getEntityById(id);
        String slug = resolveSlug(request.getSlug(), request.getName());
        if (!region.getSlug().equals(slug) && regionRepository.existsBySlug(slug)) {
            throw new ApiException("REGION_SLUG_EXISTS", "Ce slug de region existe deja", HttpStatus.CONFLICT);
        }

        region.setName(request.getName());
        region.setSlug(slug);
        region.setCode(request.getCode());
        region.setDescription(request.getDescription());
        region.setCoverImage(request.getCoverImage());
        return RegionResponse.from(regionRepository.save(region));
    }

    private String resolveSlug(String slug, String name) {
        if (slug != null && !slug.isBlank()) {
            return slug;
        }
        return SlugUtil.slugify(name);
    }
}
