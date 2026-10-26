package com.yeyamo_mobile.api.api_gateway.controller;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import com.yeyamo_mobile.api.api_gateway.filter.CorrelationIdFilter;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/fallback/{service}")
    public ResponseEntity<Map<String, Object>> unavailable(
            @PathVariable String service,
            HttpServletRequest request) {
        Throwable failure = failure(request);
        log.warn(
                "Gateway fallback service={} route={} correlationId={} exceptionType={} rootCauseType={} elapsedMs={} circuitState={}",
                service,
                value(request.getAttribute(MvcUtils.GATEWAY_ROUTE_ID_ATTR)),
                value(request.getHeader(CorrelationIdFilter.HEADER)),
                type(failure),
                type(rootCause(failure)),
                elapsedMillis(request),
                circuitState(failure));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "code", "SERVICE_UNAVAILABLE",
                "message", "Le service demandé est temporairement indisponible",
                "service", service,
                "timestamp", Instant.now().toString(),
                "correlationId", value(request.getHeader(CorrelationIdFilter.HEADER))));
    }

    private Throwable failure(HttpServletRequest request) {
        Object attribute = request.getAttribute(MvcUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        return attribute instanceof Throwable throwable ? throwable : null;
    }

    private Throwable rootCause(Throwable failure) {
        if (failure == null) {
            return null;
        }
        Throwable root = failure;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    private String type(Throwable throwable) {
        return throwable == null ? "unknown" : throwable.getClass().getSimpleName();
    }

    private String circuitState(Throwable failure) {
        Throwable root = rootCause(failure);
        if (root instanceof CallNotPermittedException) {
            return "OPEN";
        }
        if (root instanceof TimeoutException) {
            return "TIME_LIMITER_TIMEOUT";
        }
        return failure == null ? "UNKNOWN" : "EXECUTION_FAILURE";
    }

    private long elapsedMillis(HttpServletRequest request) {
        Object started = request.getAttribute(CorrelationIdFilter.START_NANOS_ATTRIBUTE);
        if (!(started instanceof Long startNanos)) {
            return -1L;
        }
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
