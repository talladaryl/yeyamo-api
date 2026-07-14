package com.yeyamo_mobile.api.catalog_service.interfaces.rest;
import com.yeyamo_mobile.api.catalog_service.domain.model.AssetStatus;
import jakarta.validation.constraints.NotNull;
public record StatusChangeRequest(@NotNull AssetStatus status){}
