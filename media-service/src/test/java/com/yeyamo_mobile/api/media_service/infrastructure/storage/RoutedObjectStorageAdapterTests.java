package com.yeyamo_mobile.api.media_service.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.media_service.application.MediaException;

class RoutedObjectStorageAdapterTests {

    @Test
    void productionRoutesNewObjectsToR2WithoutInstantiatingLocalStorage() {
        R2StorageAdapter r2 = mock(R2StorageAdapter.class);
        when(r2.store(eq("private/originals/document.pdf"), any(), eq(1L), eq("application/pdf")))
                .thenReturn("private/originals/document.pdf");
        RoutedObjectStorageAdapter storage = new RoutedObjectStorageAdapter(r2, Optional.empty());

        assertEquals("r2/private/originals/document.pdf",
                storage.store("private/originals/document.pdf", new ByteArrayInputStream(new byte[] {1}), 1,
                        "application/pdf"));
        verify(r2).store(eq("private/originals/document.pdf"), any(), eq(1L), eq("application/pdf"));
    }

    @Test
    void productionRejectsLegacyLocalKeysInsteadOfFallingBackToEitherR2Bucket() {
        RoutedObjectStorageAdapter storage = new RoutedObjectStorageAdapter(mock(R2StorageAdapter.class), Optional.empty());

        MediaException exception = assertThrows(MediaException.class, () -> storage.open("legacy/photo.png"));

        assertEquals("LEGACY_LOCAL_STORAGE_UNAVAILABLE", exception.getCode());
    }
}
