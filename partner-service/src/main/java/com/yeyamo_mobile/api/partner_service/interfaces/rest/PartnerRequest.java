package com.yeyamo_mobile.api.partner_service.interfaces.rest;
import com.yeyamo_mobile.api.partner_service.domain.model.BusinessType;import jakarta.validation.constraints.*;
public record PartnerRequest(@NotBlank@Size(max=160)String legalName,@Size(max=160)String tradeName,@NotNull BusinessType businessType,@Size(max=100)String registrationNumber,@Size(max=100)String taxId,@NotBlank@Email String contactEmail,@Size(max=40)String contactPhone,@Size(max=2048)String websiteUrl,@Size(max=1000)String description){}
