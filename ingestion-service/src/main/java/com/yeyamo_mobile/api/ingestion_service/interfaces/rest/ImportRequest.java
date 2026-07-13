package com.yeyamo_mobile.api.ingestion_service.interfaces.rest;
import com.yeyamo_mobile.api.ingestion_service.domain.model.SourceType;
import jakarta.validation.constraints.*;
public record ImportRequest(@NotNull SourceType sourceType,@Size(max=1000)String sourceReference,@Size(max=5000000)String payload){}
