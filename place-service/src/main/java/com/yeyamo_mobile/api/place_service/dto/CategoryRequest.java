package com.yeyamo_mobile.api.place_service.dto;
import jakarta.validation.constraints.NotBlank;import jakarta.validation.constraints.Size;
public record CategoryRequest(@NotBlank@Size(max=255)String name,@Size(max=255)String slug,@Size(max=255)String icon,Long parentId){}
