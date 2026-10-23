package com.yeyamo_mobile.api.ads_delivery_service.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeyamo_mobile.api.ads_delivery_service.domain.port.AdDeliveryRecordRepository;
import com.yeyamo_mobile.api.ads_delivery_service.domain.port.BudgetReservationService;
import com.yeyamo_mobile.api.ads_delivery_service.domain.port.CampaignProjectionRepository;
import com.yeyamo_mobile.api.ads_delivery_service.domain.port.FeatureFlagService;
import com.yeyamo_mobile.api.ads_delivery_service.domain.port.FrequencyCapService;
import com.yeyamo_mobile.api.ads_delivery_service.domain.port.TrackingTokenService;
import com.yeyamo_mobile.api.ads_delivery_service.infrastructure.kafka.AdsOutboxService;

class SponsoredCreativeContractTests {

    @Test
    void validCreativeIsNormalizedForFeedClients() throws Exception {
        Map<String, Object> creative = parse("{\"media\":[{\"url\":\"https://cdn.example/image.jpg\"},{\"alt\":\"missing url\"}],\"caption\":\"Hello\"}");

        assertEquals(1, ((java.util.List<?>) creative.get("media")).size());
        assertEquals("Hello", creative.get("caption"));
        assertEquals("Découvrir", creative.get("ctaLabel"));
    }

    @Test
    void malformedCreativeReturnsControlledEmptyStructureWithoutRawJson() throws Exception {
        Map<String, Object> creative = parse("not-json");

        assertTrue(((java.util.List<?>) creative.get("media")).isEmpty());
        assertNull(creative.get("caption"));
        assertEquals("Découvrir", creative.get("ctaLabel"));
        assertFalse(creative.containsKey("raw"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String json) throws Exception {
        AdDeliveryService service = new AdDeliveryService(
                mock(CampaignProjectionRepository.class),
                mock(FrequencyCapService.class),
                mock(BudgetReservationService.class),
                mock(TrackingTokenService.class),
                mock(AdDeliveryRecordRepository.class),
                mock(FeatureFlagService.class),
                mock(AdsOutboxService.class),
                new ObjectMapper());
        Method method = AdDeliveryService.class.getDeclaredMethod("parseCreative", String.class);
        method.setAccessible(true);
        return (Map<String, Object>) method.invoke(service, json);
    }
}
