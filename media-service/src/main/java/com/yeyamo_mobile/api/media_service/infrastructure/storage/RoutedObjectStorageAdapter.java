package com.yeyamo_mobile.api.media_service.infrastructure.storage;

import java.io.InputStream;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import com.yeyamo_mobile.api.media_service.application.port.ObjectStoragePort;

/** Stores all new objects in R2 while retaining read/delete compatibility for legacy local keys. */
@Component
@Primary
@ConditionalOnProperty(prefix = "r2", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RoutedObjectStorageAdapter implements ObjectStoragePort {
 private static final String R2_PREFIX="r2/";
 private final R2StorageAdapter r2; private final Optional<LocalObjectStorageAdapter> local;
 public RoutedObjectStorageAdapter(R2StorageAdapter r2,Optional<LocalObjectStorageAdapter> local){this.r2=r2;this.local=local;}
 public String store(String key,InputStream content,long length,String contentType){return R2_PREFIX+r2.store(key,content,length,contentType);}
 public StoredObject open(String key){return key.startsWith(R2_PREFIX)?r2.open(key.substring(R2_PREFIX.length())):legacyLocal().open(key);}
 public void delete(String key){if(key==null)return;if(key.startsWith(R2_PREFIX))r2.delete(key.substring(R2_PREFIX.length()));else legacyLocal().delete(key);}
 private LocalObjectStorageAdapter legacyLocal(){return local.orElseThrow(()->new com.yeyamo_mobile.api.media_service.application.MediaException("LEGACY_LOCAL_STORAGE_UNAVAILABLE","Legacy local media storage is disabled"));}
}
