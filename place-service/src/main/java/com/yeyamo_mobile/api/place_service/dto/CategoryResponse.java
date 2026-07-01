package com.yeyamo_mobile.api.place_service.dto;

import java.util.List;

import com.yeyamo_mobile.api.place_service.models.PlaceCategory;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String icon,
        Long parentId,
        List<CategoryResponse> children
) {
    public static CategoryResponse from(PlaceCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getIcon(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getChildren().stream().map(CategoryResponse::from).toList()
        );
    }

    public static CategoryResponse fromWithoutChildren(PlaceCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getIcon(),
                category.getParent() != null ? category.getParent().getId() : null,
                List.of()
        );
    }
}
