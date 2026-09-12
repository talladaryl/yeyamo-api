package com.yeyamo_mobile.api.culture_service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

import com.yeyamo_mobile.shared.country.CountryConfigClient;

class CultureRestClientConfigurationTest {

    @Test
    void applicationProvidesABuilderForCountryConfigClient() {
        new ApplicationContextRunner()
                .withPropertyValues("yeyamo.services.country-config.url=http://country-config-service")
                .withUserConfiguration(CultureHttpClientTestConfiguration.class)
                .run(context -> {
                    assertTrue(context.isRunning());
                    assertNotNull(context.getBean(RestClient.Builder.class));
                    assertNotNull(context.getBean(CountryConfigClient.class));
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(CountryConfigClient.class)
    static class CultureHttpClientTestConfiguration {
        @Bean
        RestClient.Builder restClientBuilder() {
            return new CultureServiceApplication().restClientBuilder();
        }
    }
}
