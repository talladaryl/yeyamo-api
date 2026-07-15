package com.yeyamo_mobile.api.partner_service.interfaces.rest;

import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.yeyamo_mobile.api.partner_service.application.PartnerException;

@Component
public class DocumentUploadPolicy {
    private static final Set<String> ALLOWED_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");
    private final long maximumSize;

    public DocumentUploadPolicy(@Value("${yeyamo.documents.max-size-bytes:10485760}") long maximumSize) {
        this.maximumSize = maximumSize;
    }

    public void validate(String contentType, byte[] bytes) {
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(type) || bytes.length == 0 || bytes.length > maximumSize || !matches(type, bytes)) {
            throw new PartnerException("DOCUMENT_CONTENT_INVALID", "Le contenu du document est invalide", HttpStatus.BAD_REQUEST);
        }
    }

    public String safeFilename(String filename) {
        String value = filename == null ? "document" : filename.replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).replaceAll("[^a-zA-Z0-9._-]", "_");
        return value.isBlank() ? "document" : value.substring(0, Math.min(180, value.length()));
    }

    private boolean matches(String type, byte[] bytes) {
        if ("application/pdf".equals(type)) {
            return bytes.length > 4 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D'
                    && bytes[3] == 'F' && bytes[4] == '-';
        }
        if ("image/png".equals(type)) {
            return bytes.length > 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P'
                    && bytes[2] == 'N' && bytes[3] == 'G';
        }
        return bytes.length > 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff;
    }
}
