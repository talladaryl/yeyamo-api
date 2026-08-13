package com.yeyamo_mobile.api.partner_service.infrastructure.persistence;

import java.time.Instant;import java.util.UUID;import jakarta.persistence.*;

@Entity @Table(name="country_kyc_requirements")
public class CountryKycRequirementEntity {
 @Id public UUID id; @Column(name="country_code",nullable=false,length=2) public String countryCode;
 @Column(name="partner_type",nullable=false,length=40) public String partnerType;
 @Column(name="document_type",nullable=false,length=50) public String documentType;
 @Column(nullable=false) public boolean required; @Column(name="validity_period_days") public Integer validityPeriodDays;
 @Column(columnDefinition="TEXT") public String description; @Column(nullable=false) public boolean active=true;
 @Column(name="configuration_version",nullable=false) public long configurationVersion=1;
 @Column(name="created_at",nullable=false) public Instant createdAt; @Column(name="updated_at",nullable=false) public Instant updatedAt;
}
