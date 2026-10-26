package com.yeyamo_mobile.api.api_gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.yeyamo_mobile.api.api_gateway.filter.CorrelationIdFilter;

@ExtendWith(OutputCaptureExtension.class)
class FallbackControllerTest {

    @Test
    void preservesPublicFallbackContractWhenCircuitBreakerProvidesFailureContext() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/login");
        request.addHeader(CorrelationIdFilter.HEADER, "gateway-test-123");
        request.setAttribute(MvcUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR, new TimeoutException("backend timeout"));
        request.setAttribute(CorrelationIdFilter.START_NANOS_ATTRIBUTE, System.nanoTime() - 1_000_000L);

        var response = new FallbackController().unavailable("auth-service", request);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody())
                .containsOnlyKeys("code", "message", "service", "timestamp", "correlationId")
                .containsEntry("code", "SERVICE_UNAVAILABLE")
                .containsEntry("message", "Le service demandé est temporairement indisponible")
                .containsEntry("service", "auth-service")
                .containsEntry("correlationId", "gateway-test-123");
    }

    @Test
    void doesNotLogFailureMessagesOrSecrets(CapturedOutput output) {
        String secret = "Bearer test-secret-that-must-not-be-logged";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/login");
        request.addHeader(CorrelationIdFilter.HEADER, "gateway-test-secret-safe");
        request.setAttribute(MvcUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR, new TimeoutException(secret));

        new FallbackController().unavailable("auth-service", request);

        assertThat(output.toString()).contains("exceptionType=TimeoutException")
                .doesNotContain(secret)
                .doesNotContain("Authorization")
                .doesNotContain("password");
    }
}
