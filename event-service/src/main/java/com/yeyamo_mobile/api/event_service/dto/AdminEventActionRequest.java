package com.yeyamo_mobile.api.event_service.dto;
import jakarta.validation.constraints.NotBlank;import jakarta.validation.constraints.Size;
public record AdminEventActionRequest(@NotBlank@Size(max=1000)String reason){}
