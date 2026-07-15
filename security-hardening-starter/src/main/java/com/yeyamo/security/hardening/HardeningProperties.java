package com.yeyamo.security.hardening;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("yeyamo.security")
public class HardeningProperties {
    private boolean enabled = true;
    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:3000"));
    private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    private List<String> allowedHeaders = new ArrayList<>(List.of(
            "Authorization", "Content-Type", "X-Correlation-Id", "Idempotency-Key", "If-Match"));
    private boolean allowCredentials;
    private boolean trustForwardedProto;
    private int maxUriLength = 2_048;
    private int maxQueryLength = 4_096;
    private int requestsPerMinute = 600;
    private int burstCapacity = 120;
    private int authenticationRequestsPerMinute = 20;
    private int authenticationBurstCapacity = 5;
    private int maxTrackedClients = 50_000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<String> getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    public List<String> getAllowedMethods() { return allowedMethods; }
    public void setAllowedMethods(List<String> allowedMethods) { this.allowedMethods = allowedMethods; }
    public List<String> getAllowedHeaders() { return allowedHeaders; }
    public void setAllowedHeaders(List<String> allowedHeaders) { this.allowedHeaders = allowedHeaders; }
    public boolean isAllowCredentials() { return allowCredentials; }
    public void setAllowCredentials(boolean allowCredentials) { this.allowCredentials = allowCredentials; }
    public boolean isTrustForwardedProto() { return trustForwardedProto; }
    public void setTrustForwardedProto(boolean trustForwardedProto) { this.trustForwardedProto = trustForwardedProto; }
    public int getMaxUriLength() { return maxUriLength; }
    public void setMaxUriLength(int maxUriLength) { this.maxUriLength = maxUriLength; }
    public int getMaxQueryLength() { return maxQueryLength; }
    public void setMaxQueryLength(int maxQueryLength) { this.maxQueryLength = maxQueryLength; }
    public int getRequestsPerMinute() { return requestsPerMinute; }
    public void setRequestsPerMinute(int requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }
    public int getBurstCapacity() { return burstCapacity; }
    public void setBurstCapacity(int burstCapacity) { this.burstCapacity = burstCapacity; }
    public int getAuthenticationRequestsPerMinute() { return authenticationRequestsPerMinute; }
    public void setAuthenticationRequestsPerMinute(int authenticationRequestsPerMinute) { this.authenticationRequestsPerMinute = authenticationRequestsPerMinute; }
    public int getAuthenticationBurstCapacity() { return authenticationBurstCapacity; }
    public void setAuthenticationBurstCapacity(int authenticationBurstCapacity) { this.authenticationBurstCapacity = authenticationBurstCapacity; }
    public int getMaxTrackedClients() { return maxTrackedClients; }
    public void setMaxTrackedClients(int maxTrackedClients) { this.maxTrackedClients = maxTrackedClients; }
}
