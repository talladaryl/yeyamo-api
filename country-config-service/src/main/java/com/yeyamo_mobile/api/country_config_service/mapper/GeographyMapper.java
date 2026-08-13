package com.yeyamo_mobile.api.country_config_service.mapper;

import com.yeyamo_mobile.api.country_config_service.domain.model.*;
import com.yeyamo_mobile.api.country_config_service.dto.*;
import org.springframework.stereotype.Component;

@Component
public class GeographyMapper {

    public AdministrativeAreaDto toDto(AdministrativeArea area) {
        return new AdministrativeAreaDto(
                area.getId(),
                area.getCountryCode(),
                area.getParentId(),
                area.getLevel(),
                area.getTypeCode(),
                area.getName(),
                area.getLocalizedNames(),
                area.getOfficialCode(),
                area.getSlug(),
                area.getLatitude(),
                area.getLongitude(),
                area.getActive(),
                area.getCreatedAt(),
                area.getUpdatedAt()
        );
    }

    public AdministrativeLevelLabelDto toDto(AdministrativeLevelLabel label) {
        return new AdministrativeLevelLabelDto(
                label.getId(),
                label.getCountryCode(),
                label.getLevel(),
                label.getLabel(),
                label.getLabelPlural(),
                label.getLocalizedLabels(),
                label.getDisplayOrder()
        );
    }

    public CityDto toDto(City city) {
        return new CityDto(
                city.getId(),
                city.getCountryCode(),
                city.getAdministrativeAreaId(),
                city.getName(),
                city.getSlug(),
                city.getLatitude(),
                city.getLongitude(),
                city.getActive(),
                city.getPopulation(),
                city.getCreatedAt(),
                city.getUpdatedAt()
        );
    }

    public LocalityDto toDto(Locality locality) {
        return new LocalityDto(
                locality.getId(),
                locality.getCountryCode(),
                locality.getCityId(),
                locality.getAdministrativeAreaId(),
                locality.getLocalityType(),
                locality.getName(),
                locality.getSlug(),
                locality.getLatitude(),
                locality.getLongitude(),
                locality.getActive(),
                locality.getCreatedAt(),
                locality.getUpdatedAt()
        );
    }
}
