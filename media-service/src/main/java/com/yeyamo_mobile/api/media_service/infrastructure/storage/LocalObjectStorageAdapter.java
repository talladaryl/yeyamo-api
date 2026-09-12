package com.yeyamo_mobile.api.media_service.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.media_service.application.MediaException;
import com.yeyamo_mobile.api.media_service.application.port.ObjectStoragePort;

@Component
@ConditionalOnProperty(prefix = "media.storage.local", name = "enabled", havingValue = "true")
public class LocalObjectStorageAdapter implements ObjectStoragePort {
    private final Path root;

    public LocalObjectStorageAdapter(@Value("${media.storage.local.root}") String configuredRoot) {
        try {
            Path candidate = Path.of(configuredRoot).toAbsolutePath().normalize();
            Files.createDirectories(candidate);
            this.root = candidate.toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (IOException failure) {
            throw new IllegalStateException("Unable to initialize media storage", failure);
        }
    }

    @Override
    public String store(String key, InputStream content, long length, String contentType) {
        Path target = confined(key);
        Path temporary = null;
        try {
            ensureSafeParents(target.getParent());
            temporary = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
            long copied = Files.copy(content, temporary, StandardCopyOption.REPLACE_EXISTING);
            if (copied != length) {
                throw new MediaException("INVALID_MEDIA_SIZE", "Stored length does not match declared length");
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            return root.relativize(target).toString().replace('\\', '/');
        } catch (IOException failure) {
            throw new MediaException("MEDIA_STORAGE_FAILURE", "Media could not be stored");
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // Best-effort cleanup without leaking the local path.
                }
            }
        }
    }

    @Override
    public StoredObject open(String key) {
        Path target = confined(key);
        try {
            if (Files.isSymbolicLink(target) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new MediaException("MEDIA_NOT_FOUND", "Media not found");
            }
            return new StoredObject(Files.newInputStream(target), Files.size(target), "application/octet-stream");
        } catch (IOException failure) {
            throw new MediaException("MEDIA_NOT_FOUND", "Media not found");
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(confined(key));
        } catch (IOException failure) {
            throw new MediaException("MEDIA_STORAGE_FAILURE", "Media could not be deleted");
        }
    }

    private Path confined(String key) {
        if (key == null || key.isBlank() || key.indexOf('\0') >= 0) {
            throw new MediaException("INVALID_STORAGE_KEY", "Invalid media storage key");
        }
        Path target = root.resolve(key.replace('\\', '/')).normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            throw new MediaException("INVALID_STORAGE_KEY", "Invalid media storage key");
        }
        return target;
    }

    private void ensureSafeParents(Path parent) throws IOException {
        Path current = root;
        for (Path segment : root.relativize(parent)) {
            current = current.resolve(segment);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
                throw new MediaException("INVALID_STORAGE_KEY", "Symbolic links are not allowed in media storage");
            }
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(current);
            }
        }
    }
}
