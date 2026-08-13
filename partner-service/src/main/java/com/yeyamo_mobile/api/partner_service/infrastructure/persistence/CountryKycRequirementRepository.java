package com.yeyamo_mobile.api.partner_service.infrastructure.persistence;
import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface CountryKycRequirementRepository extends JpaRepository<CountryKycRequirementEntity,UUID>{
 List<CountryKycRequirementEntity> findByCountryCodeAndPartnerTypeAndActiveTrueOrderByDocumentType(String countryCode,String partnerType);
}
