package com.yeyamo_mobile.api.partner_service.infrastructure.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilesystemDocumentStorageAdapterTest {

    @Test
    void createsConfiguredRootAndStoresDocumentsWithinIt(@TempDir Path tempDir) throws Exception {
        Path root = tempDir.resolve("partner-documents");
        FilesystemDocumentStorageAdapter adapter = new FilesystemDocumentStorageAdapter(root.toString());

        String key = adapter.store("partner-1", "identity.pdf", "application/pdf",
                new ByteArrayInputStream("document".getBytes(StandardCharsets.UTF_8)));

        assertThat(Files.exists(root)).isTrue();
        assertThat(key).startsWith("partner-1/").endsWith(".pdf");
        assertThat(new String(adapter.open(key).readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("document");
        assertThatThrownBy(() -> adapter.open("../outside"))
                .isInstanceOf(SecurityException.class);
    }
}
