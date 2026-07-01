package com.yeyamo_mobile.api.place_service.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.place_service.dto.CategoryResponse;
import com.yeyamo_mobile.api.place_service.exception.ApiException;
import com.yeyamo_mobile.api.place_service.models.PlaceCategory;
import com.yeyamo_mobile.api.place_service.repository.PlaceCategoryRepository;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final PlaceCategoryRepository categoryRepository;

    public CategoryService(PlaceCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> listCategories() {
        return categoryRepository.findByParentIsNullOrderByNameAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public PlaceCategory getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException("CATEGORY_NOT_FOUND", "Categorie introuvable", HttpStatus.NOT_FOUND));
    }
}
