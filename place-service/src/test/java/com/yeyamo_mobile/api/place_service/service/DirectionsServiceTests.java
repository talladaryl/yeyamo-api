package com.yeyamo_mobile.api.place_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.yeyamo_mobile.api.place_service.config.OpenRouteServiceProperties;
import com.yeyamo_mobile.api.place_service.dto.DirectionsResponse;
import com.yeyamo_mobile.api.place_service.exception.ApiException;

class DirectionsServiceTests {
    private static final String URL = "https://ors.test/v2/directions/driving-car?start=11.5,3.5&end=11.6,3.6";

    @Test
    void returnsSimplifiedRouteAndCachesSameRequest() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(URL)).andExpect(method(HttpMethod.GET)).andExpect(header("Authorization", "test-key"))
                .andRespond(withSuccess(response(), MediaType.APPLICATION_JSON));

        DirectionsResponse first = fixture.service.directions(3.5, 11.5, 3.6, 11.6, "driving-car");
        DirectionsResponse second = fixture.service.directions(3.5000001, 11.5000001, 3.6000001, 11.6000001, "driving-car");

        assertEquals(1234d, first.distanceMeters());
        assertEquals(first, second);
        fixture.server.verify();
    }

    @Test
    void mapsQuotaAndTimeoutToRoutingUnavailable() {
        Fixture quota = fixture();
        quota.server.expect(once(), requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertCode(() -> quota.service.directions(3.5, 11.5, 3.6, 11.6, "driving-car"));

        Fixture timeout = fixture();
        timeout.server.expect(once(), requestTo(URL)).andRespond(withException(new java.io.IOException("timeout")));
        assertCode(() -> timeout.service.directions(3.5, 11.5, 3.6, 11.6, "driving-car"));
    }

    private void assertCode(Runnable call) {
        assertEquals("ROUTING_UNAVAILABLE", assertThrows(ApiException.class, call::run).getCode());
    }

    private Fixture fixture() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ors.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenRouteServiceProperties properties = new OpenRouteServiceProperties(URI.create("https://ors.test"), "test-key", Duration.ofSeconds(1), Duration.ofMinutes(5));
        return new Fixture(new DirectionsService(builder.build(), properties, new com.fasterxml.jackson.databind.ObjectMapper()), server);
    }

    private String response() {
        return "{\"features\":[{\"properties\":{\"summary\":{\"distance\":1234,\"duration\":321}},\"geometry\":{\"coordinates\":[[11.5,3.5],[11.6,3.6]]}}]}";
    }

    private record Fixture(DirectionsService service, MockRestServiceServer server) { }
}
