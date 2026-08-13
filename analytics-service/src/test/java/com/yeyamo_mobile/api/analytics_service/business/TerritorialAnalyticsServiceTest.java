package com.yeyamo_mobile.api.analytics_service.business;

import static org.mockito.ArgumentMatchers.any;import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;import java.time.Instant;import java.util.*;import org.junit.jupiter.api.Test;

class TerritorialAnalyticsServiceTest {
 @Test void revenue_is_kept_separate_byCurrency() throws Exception {
  var countries=mock(CountryActivityDailyRepository.class);var cities=mock(CityActivityDailyRepository.class);var revenue=mock(CountryRevenueDailyRepository.class);var culture=mock(CultureCountryDailyRepository.class);var artisan=mock(ArtisanCountryDailyRepository.class);
  when(countries.findByStatDateAndCountryCodeAndEventType(any(),any(),any())).thenReturn(Optional.empty());when(revenue.findByStatDateAndCountryCodeAndCurrencyCode(any(),any(),any())).thenReturn(Optional.empty());
  TerritorialAnalyticsService service=new TerritorialAnalyticsService(countries,cities,revenue,culture,artisan);ObjectMapper json=new ObjectMapper();
  StoredAnalyticsEvent xaf=new StoredAnalyticsEvent();xaf.eventType="payment.confirmed";xaf.occurredAt=Instant.now();xaf.countryCode="CM";xaf.contentCountryCode="CM";xaf.currencyCode="XAF";
  StoredAnalyticsEvent ngn=new StoredAnalyticsEvent();ngn.eventType="payment.confirmed";ngn.occurredAt=Instant.now();ngn.countryCode="NG";ngn.contentCountryCode="NG";ngn.currencyCode="NGN";
  service.record(xaf,json.readTree("{\"amount\":1000}"));service.record(ngn,json.readTree("{\"amount\":2000}"));
  verify(revenue,times(2)).save(any(CountryRevenueDaily.class));verify(revenue).findByStatDateAndCountryCodeAndCurrencyCode(any(),eq("CM"),eq("XAF"));verify(revenue).findByStatDateAndCountryCodeAndCurrencyCode(any(),eq("NG"),eq("NGN"));
 }
}
