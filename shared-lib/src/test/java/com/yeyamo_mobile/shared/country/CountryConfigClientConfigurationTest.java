package com.yeyamo_mobile.shared.country;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class CountryConfigClientConfigurationTest {

    @Test
    void defaultUrlUsesTheComposeApplicationPort() throws Exception {
        CountryConfigClient client = new CountryConfigClient(RestClient.builder(), CountryConfigClient.DEFAULT_SERVICE_URL);

        assertThat(serviceUrl(client)).isEqualTo("http://country-config-service:8117");
    }

    @Test
    void explicitConfigurationStillOverridesTheDefault() throws Exception {
        CountryConfigClient client = new CountryConfigClient(RestClient.builder(), "http://country-config-service:19117/");

        assertThat(serviceUrl(client)).isEqualTo("http://country-config-service:19117");
    }

    private String serviceUrl(CountryConfigClient client) throws Exception {
        Field field = CountryConfigClient.class.getDeclaredField("serviceUrl");
        field.setAccessible(true);
        return (String) field.get(client);
    }
}
