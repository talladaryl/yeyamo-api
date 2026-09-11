package com.yeyamo_mobile.api.payment_service.infrastructure.provider;

import com.yeyamo_mobile.api.payment_service.application.port.PaymentProviderPort;
import java.util.List;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public final class ProductionPaymentProviderGuard implements SmartInitializingSingleton {
    private final String configuredProvider;
    private final List<PaymentProviderPort> providers;

    public ProductionPaymentProviderGuard(
            @Value("${payment.provider.name:}") String configuredProvider,
            List<PaymentProviderPort> providers) {
        this.configuredProvider = configuredProvider == null ? "" : configuredProvider.trim();
        this.providers = providers;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (configuredProvider.isBlank()) {
            throw new IllegalStateException("EXTERNAL_PROVIDER_REQUIRED: payment.provider.name is missing");
        }
        if ("simulated".equalsIgnoreCase(configuredProvider)) {
            throw new IllegalStateException("EXTERNAL_PROVIDER_REQUIRED: simulated provider is forbidden in production");
        }
        boolean available = providers.stream()
            .anyMatch(provider -> configuredProvider.equalsIgnoreCase(provider.name()));
        if (!available) {
            throw new IllegalStateException("EXTERNAL_PROVIDER_REQUIRED: configured payment provider is unavailable");
        }
    }
}
