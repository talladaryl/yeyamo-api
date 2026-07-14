package com.yeyamo_mobile.api.media_service.application.port;
import java.io.InputStream;
public interface ObjectStoragePort{
 String store(String key,InputStream content,long length,String contentType);
 StoredObject open(String key);void delete(String key);
 record StoredObject(InputStream content,long length,String contentType){}
}
