package com.yeyamo_mobile.api.analytics_service.business;

import java.math.BigDecimal;import java.time.*;import java.util.UUID;import jakarta.persistence.*;

@Entity @Table(name="country_activity_daily") class CountryActivityDaily { @Id UUID id; LocalDate statDate; String countryCode; String eventType; long events; long views; long interactions; Instant updatedAt; }
@Entity @Table(name="city_activity_daily") class CityActivityDaily { @Id UUID id; LocalDate statDate; String countryCode; String cityId; String eventType; long events; long views; long interactions; Instant updatedAt; }
@Entity @Table(name="country_revenue_daily") class CountryRevenueDaily { @Id UUID id; LocalDate statDate; String countryCode; String currencyCode; long payments; BigDecimal revenue=BigDecimal.ZERO; BigDecimal refunds=BigDecimal.ZERO; Instant updatedAt; }
@Entity @Table(name="culture_country_daily") class CultureCountryDaily { @Id UUID id; LocalDate statDate; String countryCode; String contentType; long events; long views; long interactions; Instant updatedAt; }
@Entity @Table(name="artisan_country_daily") class ArtisanCountryDaily { @Id UUID id; LocalDate statDate; String countryCode; long events; long views; long interactions; Instant updatedAt; }
