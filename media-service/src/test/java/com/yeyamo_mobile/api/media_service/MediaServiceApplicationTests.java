package com.yeyamo_mobile.api.media_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.yeyamo_mobile.api.media_service.application.port.ObjectStoragePort;
import com.yeyamo_mobile.api.media_service.infrastructure.storage.LocalObjectStorageAdapter;
import com.yeyamo_mobile.api.media_service.infrastructure.storage.RoutedObjectStorageAdapter;

@SpringBootTest
class MediaServiceApplicationTests {
    @Autowired ApplicationContext context;
    @Autowired ObjectStoragePort storage;

    @Test
    void r2ProductionConfigurationDoesNotInstantiateLocalStorage() {
        assertInstanceOf(RoutedObjectStorageAdapter.class, storage);
        assertEquals(0, context.getBeansOfType(LocalObjectStorageAdapter.class).size());
    }
}
