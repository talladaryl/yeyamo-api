package com.yeyamo_mobile.api.payment_service.infrastructure.aggregator;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payment.aggregator")
public class HrSkillsPayProperties {
    private String baseUrl = "";
    private String keyA = "";
    private String keyB = "";
    private String webhookSecret = "";
    /** JSON capability map, e.g. {"CM":["mtn","orange"]}; deployment-owned. */
    private String cashInCapabilities = "";

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getKeyA() { return keyA; }
    public void setKeyA(String keyA) { this.keyA = keyA; }

    public String getKeyB() { return keyB; }
    public void setKeyB(String keyB) { this.keyB = keyB; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public String getCashInCapabilities() { return cashInCapabilities; }
    public void setCashInCapabilities(String cashInCapabilities) { this.cashInCapabilities = cashInCapabilities; }
}
