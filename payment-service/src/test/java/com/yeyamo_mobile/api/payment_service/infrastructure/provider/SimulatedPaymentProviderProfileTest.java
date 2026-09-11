package com.yeyamo_mobile.api.payment_service.infrastructure.provider;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import com.yeyamo_mobile.api.payment_service.application.port.PaymentProviderPort;

class SimulatedPaymentProviderProfileTest {
    private ApplicationContextRunner context(String profile, String provider) {
        ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SimulatedPaymentProvider.class)
            .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile));
        return provider == null ? runner : runner.withPropertyValues("payment.provider.name=" + provider);
    }

    @Test void productionRejectsSimulated() { context("prod", "simulated").run(c -> assertFalse(c.containsBean("simulatedPaymentProvider"))); }
    @Test void productionRejectsMissingProvider() { context("prod", null).run(c -> assertFalse(c.containsBean("simulatedPaymentProvider"))); }
    @Test void productionRejectsUnknownProvider() { context("prod", "unknown").run(c -> assertFalse(c.containsBean("simulatedPaymentProvider"))); }
    @Test void developmentAllowsExplicitSimulated() { context("dev", "simulated").run(c -> assertTrue(c.containsBean("simulatedPaymentProvider"))); }
    @Test void testAllowsExplicitSimulated() { context("test", "simulated").run(c -> assertTrue(c.containsBean("simulatedPaymentProvider"))); }

    private ApplicationContextRunner production(String provider) {
        ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ProductionPaymentProviderGuard.class)
            .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"));
        return provider == null ? runner : runner.withPropertyValues("payment.provider.name=" + provider);
    }

    @Test void productionStartupFailsWithSimulated() {
        production("simulated").run(c -> assertNotNull(c.getStartupFailure()));
    }

    @Test void productionStartupFailsWithoutProvider() {
        production(null).run(c -> assertNotNull(c.getStartupFailure()));
    }

    @Test void productionStartupFailsWithUnknownProvider() {
        production("unknown").run(c -> assertNotNull(c.getStartupFailure()));
    }

    @Test void productionResolvesConfiguredHrSkillsPayProvider() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(ProductionPaymentProviderGuard.class, HrSkillsPayProvider.class,
                com.yeyamo_mobile.api.payment_service.infrastructure.aggregator.HrSkillsPayProperties.class)
            .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
            .withPropertyValues(
                "payment.provider.name=hr-skills-pay",
                "payment.aggregator.base-url=https://api.hrskills-pay.com",
                "payment.aggregator.key-a=set-for-test",
                "payment.aggregator.key-b=set-for-test",
                "payment.aggregator.webhook-secret=01234567890123456789012345678901",
                "payment.aggregator.cash-in-capabilities={\"CI\":[\"orange\"]}")
            .run(c -> {
                assertNull(c.getStartupFailure());
                assertEquals("hr-skills-pay", c.getBean(PaymentProviderPort.class).name());
            });
    }
}
