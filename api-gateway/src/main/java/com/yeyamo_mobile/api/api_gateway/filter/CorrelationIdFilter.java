package com.yeyamo_mobile.api.api_gateway.filter;

import java.io.IOException;
import java.util.UUID;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = normalize(request.getHeader(HEADER));
        response.setHeader(HEADER, correlationId);
        MDC.put(MDC_KEY, correlationId);
        try {
            chain.doFilter(new CorrelationRequest(request, correlationId), response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private static final class CorrelationRequest extends HttpServletRequestWrapper {
        private final String correlationId;
        CorrelationRequest(HttpServletRequest request,String correlationId){super(request);this.correlationId=correlationId;}
        @Override public String getHeader(String name){return HEADER.equalsIgnoreCase(name)?correlationId:super.getHeader(name);}
        @Override public Enumeration<String> getHeaders(String name){return HEADER.equalsIgnoreCase(name)?Collections.enumeration(List.of(correlationId)):super.getHeaders(name);}
        @Override public Enumeration<String> getHeaderNames(){LinkedHashSet<String> names=new LinkedHashSet<>(Collections.list(super.getHeaderNames()));names.add(HEADER);return Collections.enumeration(names);}
    }

    private String normalize(String candidate) {
        if (candidate == null || candidate.isBlank() || candidate.length() > 100
                || !candidate.matches("[A-Za-z0-9._:-]+")) {
            return UUID.randomUUID().toString();
        }
        return candidate;
    }
}
