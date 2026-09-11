package com.yeyamo_mobile.api.payment_service.infrastructure.provider;

import com.yeyamo_mobile.api.payment_service.application.port.PaymentProviderPort;
import com.yeyamo_mobile.api.payment_service.infrastructure.aggregator.HrSkillsPayProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Production selection bean for the HR-Skills Pay command/webhook integration.
 * Cash-in is initiated by HrSkillsPayService because the provider requires
 * operator/country/phone fields carried by the payment command.
 */
@Component
@Profile({"prod", "test"})
@ConditionalOnProperty(name = "payment.provider.name", havingValue = "hr-skills-pay")
public final class HrSkillsPayProvider implements PaymentProviderPort, SmartInitializingSingleton {
    private final HrSkillsPayProperties properties;
    private final Environment environment;

    public HrSkillsPayProvider(HrSkillsPayProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public String name() {
        return "hr-skills-pay";
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod")) return;
        if (blank(properties.getKeyA()) || blank(properties.getKeyB())
                || blank(properties.getWebhookSecret())) {
            throw new IllegalStateException("EXTERNAL_PROVIDER_REQUIRED: HR-Skills Pay credentials are incomplete");
        }
        if (blank(properties.getBaseUrl()) || !properties.getBaseUrl().startsWith("https://")) {
            throw new IllegalStateException("EXTERNAL_PROVIDER_REQUIRED: HR-Skills Pay production URL must use HTTPS");
        }
        if (blank(properties.getCashInCapabilities())) {
            throw new IllegalStateException("EXTERNAL_PROVIDER_CAPABILITIES_REQUIRED: configure contracted country/operator pairs");
        }
    }

    @Override
    public ProviderResult authorize(AuthorizationRequest request) {
        return new ProviderResult(Outcome.FAILED, null,
            "HR-Skills Pay authorization requires the provider-specific command fields");
    }

    @Override
    public ProviderResult cancel(String providerPaymentId, String idempotencyKey) {
        return new ProviderResult(Outcome.FAILED, providerPaymentId,
            "HR-Skills Pay cancellation is not available through the generic provider port");
    }

    @Override
    public ProviderResult refund(RefundRequest request) {
        return new ProviderResult(Outcome.FAILED, null,
            "HR-Skills Pay refund is not available through the generic provider port");
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
