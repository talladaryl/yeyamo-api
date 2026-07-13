package com.yeyamo_mobile.api.partner_service.application.port;
import java.io.IOException;import java.io.InputStream;
public interface DocumentStoragePort { String store(String ownerKey,String filename,String contentType,InputStream content) throws IOException; void delete(String storageKey) throws IOException; }
