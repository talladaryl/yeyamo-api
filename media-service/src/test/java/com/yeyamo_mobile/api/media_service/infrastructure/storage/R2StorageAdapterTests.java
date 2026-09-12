package com.yeyamo_mobile.api.media_service.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.yeyamo_mobile.api.media_service.application.MediaException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class R2StorageAdapterTests {
    private S3Client client;
    private R2StorageAdapter storage;

    @BeforeEach
    void setUp() {
        client = mock(S3Client.class);
        R2StorageProperties properties = new R2StorageProperties();
        properties.setPublicBucketName("public-bucket");
        properties.setPrivateBucketName("private-bucket");
        storage = new R2StorageAdapter(client, properties);
    }

    @Test
    void uploadsPublicAndPrivateObjectsToTheirOwnBuckets() {
        storage.store("public/originals/photo.png", new ByteArrayInputStream(new byte[] {1}), 1, "image/png");
        storage.store("private/originals/id.pdf", new ByteArrayInputStream(new byte[] {2}), 1, "application/pdf");

        ArgumentCaptor<PutObjectRequest> requests = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client, org.mockito.Mockito.times(2)).putObject(requests.capture(), any(RequestBody.class));
        assertEquals("public-bucket", requests.getAllValues().get(0).bucket());
        assertEquals("originals/photo.png", requests.getAllValues().get(0).key());
        assertEquals("private-bucket", requests.getAllValues().get(1).bucket());
        assertEquals("originals/id.pdf", requests.getAllValues().get(1).key());
    }

    @Test
    void readsPublicAndPrivateObjectsFromTheirOwnBuckets() throws Exception {
        when(client.getObject(any(GetObjectRequest.class))).thenAnswer(invocation -> response(new byte[] {7}));

        try (InputStream publicContent = storage.open("public/originals/photo.png").content();
                InputStream privateContent = storage.open("private/originals/id.pdf").content()) {
            assertArrayEquals(new byte[] {7}, publicContent.readAllBytes());
            assertArrayEquals(new byte[] {7}, privateContent.readAllBytes());
        }

        ArgumentCaptor<GetObjectRequest> requests = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(client, org.mockito.Mockito.times(2)).getObject(requests.capture());
        assertEquals("public-bucket", requests.getAllValues().get(0).bucket());
        assertEquals("private-bucket", requests.getAllValues().get(1).bucket());
    }

    @Test
    void deletesPublicAndPrivateObjectsFromTheirOwnBuckets() {
        storage.delete("public/thumbnails/photo.jpg");
        storage.delete("private/thumbnails/id.jpg");

        ArgumentCaptor<DeleteObjectRequest> requests = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(client, org.mockito.Mockito.times(2)).deleteObject(requests.capture());
        assertEquals("public-bucket", requests.getAllValues().get(0).bucket());
        assertEquals("private-bucket", requests.getAllValues().get(1).bucket());
    }

    @Test
    void rejectsUnclassifiedR2KeysInsteadOfFallingBackToEitherBucket() {
        MediaException exception = assertThrows(MediaException.class,
                () -> storage.store("originals/legacy.png", new ByteArrayInputStream(new byte[] {1}), 1, "image/png"));
        assertEquals("INVALID_STORAGE_KEY", exception.getCode());
    }

    private ResponseInputStream<GetObjectResponse> response(byte[] bytes) {
        return new ResponseInputStream<>(GetObjectResponse.builder().contentLength((long) bytes.length)
                .contentType("application/octet-stream").build(), new ByteArrayInputStream(bytes));
    }
}
