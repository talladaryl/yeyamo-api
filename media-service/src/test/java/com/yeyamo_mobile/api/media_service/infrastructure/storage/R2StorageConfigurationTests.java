package com.yeyamo_mobile.api.media_service.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class R2StorageConfigurationTests {
    @Test
    void acceptsBothDedicatedBucketNames() {
        R2StorageProperties properties = configuredProperties();
        assertEquals("public-bucket", properties.getPublicBucketName());
        assertEquals("private-bucket", properties.getPrivateBucketName());
        assertDoesNotThrow(() -> new R2StorageConfiguration().r2S3Client(properties).close());
    }

    @Test
    void rejectsMissingPublicBucket() {
        R2StorageProperties properties = configuredProperties();
        properties.setPublicBucketName(null);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new R2StorageConfiguration().r2S3Client(properties));
        assertEquals("R2_PUBLIC_BUCKET_NAME must be configured", exception.getMessage());
    }

    @Test
    void rejectsMissingPrivateBucket() {
        R2StorageProperties properties = configuredProperties();
        properties.setPrivateBucketName(null);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new R2StorageConfiguration().r2S3Client(properties));
        assertEquals("R2_PRIVATE_BUCKET_NAME must be configured", exception.getMessage());
    }

    private R2StorageProperties configuredProperties() {
        R2StorageProperties properties = new R2StorageProperties();
        properties.setAccountId("test-account");
        properties.setAccessKeyId("test-access-key");
        properties.setSecretAccessKey("test-secret-key");
        properties.setPublicBucketName("public-bucket");
        properties.setPrivateBucketName("private-bucket");
        return properties;
    }
}
