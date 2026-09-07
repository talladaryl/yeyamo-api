package com.yeyamo_mobile.api.media_service.infrastructure.storage;

import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.yeyamo_mobile.api.media_service.application.MediaException;
import com.yeyamo_mobile.api.media_service.application.port.ObjectStoragePort;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class R2StorageAdapter {
 private static final Logger log=LoggerFactory.getLogger(R2StorageAdapter.class);
 private final S3Client client; private final R2StorageProperties properties;
 public R2StorageAdapter(S3Client client,R2StorageProperties properties){this.client=client;this.properties=properties;}
 public String store(String key,InputStream content,long length,String contentType){
  try{
   client.putObject(PutObjectRequest.builder().bucket(properties.getBucketName()).key(key).contentType(contentType).contentLength(length).build(),RequestBody.fromInputStream(content,length));
   return key;
  }catch(SdkException exception){throw unavailable(exception);}
 }
 public ObjectStoragePort.StoredObject open(String key){
  try{
   ResponseInputStream<GetObjectResponse> response=client.getObject(GetObjectRequest.builder().bucket(properties.getBucketName()).key(key).build());
   Long length=response.response().contentLength();
   return new ObjectStoragePort.StoredObject(response,length==null?0L:length,response.response().contentType());
  }catch(SdkException exception){throw unavailable(exception);}
 }
 public void delete(String key){
  try{client.deleteObject(builder->builder.bucket(properties.getBucketName()).key(key));}
  catch(SdkException exception){throw unavailable(exception);}
 }
 private MediaException unavailable(SdkException cause){log.warn("R2 object storage request failed: {}",cause.getMessage());return new MediaException("STORAGE_UNAVAILABLE","Media storage is temporarily unavailable",cause);}
}
