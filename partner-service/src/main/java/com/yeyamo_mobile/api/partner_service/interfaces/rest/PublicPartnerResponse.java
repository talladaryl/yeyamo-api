package com.yeyamo_mobile.api.partner_service.interfaces.rest;
import java.util.UUID;import com.yeyamo_mobile.api.partner_service.domain.model.*;
public record PublicPartnerResponse(UUID id,String name,BusinessType businessType,String websiteUrl,String description){public static PublicPartnerResponse from(Partner p){return new PublicPartnerResponse(p.getId(),p.getTradeName()==null?p.getLegalName():p.getTradeName(),p.getBusinessType(),p.getWebsiteUrl(),p.getDescription());}}
