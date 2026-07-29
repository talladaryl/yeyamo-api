package com.yeyamo_mobile.api.place_service.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import com.yeyamo_mobile.api.place_service.dto.CategoryRequest;
import com.yeyamo_mobile.api.place_service.dto.ReferenceStatusRequest;

import com.yeyamo_mobile.api.place_service.dto.CategoryResponse;
import com.yeyamo_mobile.api.place_service.service.CategoryService;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> listCategories() {
        return categoryService.listCategories();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") public CategoryResponse create(@Valid@RequestBody CategoryRequest request){return categoryService.create(request);}
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") public CategoryResponse update(@PathVariable Long id,@Valid@RequestBody CategoryRequest request){return categoryService.update(id,request);}
    @PatchMapping("/{id}/status") @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')") public CategoryResponse status(@PathVariable Long id,@RequestBody ReferenceStatusRequest request){return categoryService.setActive(id,request.active());}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('SUPER_ADMIN')") public void delete(@PathVariable Long id){categoryService.delete(id);}
}
