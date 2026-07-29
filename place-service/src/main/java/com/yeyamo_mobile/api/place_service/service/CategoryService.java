package com.yeyamo_mobile.api.place_service.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeyamo_mobile.api.place_service.dto.CategoryResponse;
import com.yeyamo_mobile.api.place_service.dto.CategoryRequest;
import com.yeyamo_mobile.api.place_service.exception.ApiException;
import com.yeyamo_mobile.api.place_service.models.PlaceCategory;
import com.yeyamo_mobile.api.place_service.repository.PlaceCategoryRepository;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final PlaceCategoryRepository categoryRepository;
    private final com.yeyamo_mobile.api.place_service.repository.PlaceRepository placeRepository;

    public CategoryService(PlaceCategoryRepository categoryRepository,com.yeyamo_mobile.api.place_service.repository.PlaceRepository placeRepository) {
        this.categoryRepository = categoryRepository;
        this.placeRepository=placeRepository;
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
    @Transactional public CategoryResponse create(CategoryRequest request){String slug=com.yeyamo_mobile.api.place_service.util.SlugUtil.slugify(request.slug()==null?request.name():request.slug());if(categoryRepository.existsBySlug(slug))throw new ApiException("CATEGORY_SLUG_EXISTS","Cette categorie existe deja",HttpStatus.CONFLICT);PlaceCategory category=new PlaceCategory();apply(category,request,slug);return CategoryResponse.from(categoryRepository.save(category));}
    @Transactional public CategoryResponse update(Long id,CategoryRequest request){PlaceCategory category=getById(id);String slug=com.yeyamo_mobile.api.place_service.util.SlugUtil.slugify(request.slug()==null?request.name():request.slug());if(!category.getSlug().equals(slug)&&categoryRepository.existsBySlug(slug))throw new ApiException("CATEGORY_SLUG_EXISTS","Cette categorie existe deja",HttpStatus.CONFLICT);apply(category,request,slug);return CategoryResponse.from(categoryRepository.save(category));}
    @Transactional public CategoryResponse setActive(Long id,boolean active){PlaceCategory category=getById(id);category.setActive(active);return CategoryResponse.from(categoryRepository.save(category));}
    @Transactional public void delete(Long id){PlaceCategory category=getById(id);if(placeRepository.existsByCategoryId(id)||categoryRepository.existsByParentId(id))throw new ApiException("CATEGORY_IN_USE","La categorie est referencee et ne peut pas etre supprimee",HttpStatus.CONFLICT);categoryRepository.delete(category);}
    private void apply(PlaceCategory category,CategoryRequest request,String slug){category.setName(request.name());category.setSlug(slug);category.setIcon(request.icon());category.setParent(request.parentId()==null?null:getById(request.parentId()));}
}
