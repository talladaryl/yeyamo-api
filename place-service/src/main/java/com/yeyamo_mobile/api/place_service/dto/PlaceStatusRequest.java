package com.yeyamo_mobile.api.place_service.dto;
import com.yeyamo_mobile.api.place_service.enums.PlaceStatus;import jakarta.validation.constraints.NotNull;import jakarta.validation.constraints.Size;
public record PlaceStatusRequest(@NotNull PlaceStatus status,boolean verified,@Size(max=1000)String reason){}
