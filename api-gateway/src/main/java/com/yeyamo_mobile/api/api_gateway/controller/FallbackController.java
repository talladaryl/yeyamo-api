package com.yeyamo_mobile.api.api_gateway.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.api_gateway.filter.CorrelationIdFilter;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/{service}")
    public ResponseEntity<Map<String, Object>> unavailable(
            @PathVariable String service,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "code", "SERVICE_UNAVAILABLE",
                "message", "Le service demandé est temporairement indisponible",
                "service", service,
                "timestamp", Instant.now().toString(),
                "correlationId", value(request.getHeader(CorrelationIdFilter.HEADER))));
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
