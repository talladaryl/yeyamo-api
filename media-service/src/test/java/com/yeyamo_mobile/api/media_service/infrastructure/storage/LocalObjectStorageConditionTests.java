package com.yeyamo_mobile.api.media_service.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class LocalObjectStorageConditionTests {
    @TempDir Path root;

    @Test
    void localStorageIsAvailableOnlyWhenExplicitlyEnabled() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(LocalStorageConfiguration.class)
                .withPropertyValues("media.storage.local.root=" + root);

        runner.run(context -> assertEquals(0, context.getBeansOfType(LocalObjectStorageAdapter.class).size()));
        runner.withPropertyValues("media.storage.local.enabled=true")
                .run(context -> assertEquals(1, context.getBeansOfType(LocalObjectStorageAdapter.class).size()));
    }

    @Configuration(proxyBeanMethods = false)
    @Import(LocalObjectStorageAdapter.class)
    static class LocalStorageConfiguration {
    }
}
