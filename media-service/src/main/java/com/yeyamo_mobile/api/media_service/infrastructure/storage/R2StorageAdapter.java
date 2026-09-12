package com.yeyamo_mobile.api.media_service.infrastructure.storage;

import java.io.InputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@ConditionalOnProperty(prefix = "r2", name = "enabled", havingValue = "true", matchIfMissing = true)
public class R2StorageAdapter {
 private static final Logger log=LoggerFactory.getLogger(R2StorageAdapter.class);
 private final S3Client client; private final R2StorageProperties properties;
 public R2StorageAdapter(S3Client client,R2StorageProperties properties){this.client=client;this.properties=properties;}
 public String store(String key,InputStream content,long length,String contentType){
  try{
   BucketObject object=bucketObject(key);
   client.putObject(PutObjectRequest.builder().bucket(object.bucket()).key(object.key()).contentType(contentType).contentLength(length).build(),RequestBody.fromInputStream(content,length));
   return key;
  }catch(SdkException exception){throw unavailable(exception);}
 }
 public ObjectStoragePort.StoredObject open(String key){
  try{
   BucketObject object=bucketObject(key);
   ResponseInputStream<GetObjectResponse> response=client.getObject(GetObjectRequest.builder().bucket(object.bucket()).key(object.key()).build());
   Long length=response.response().contentLength();
   return new ObjectStoragePort.StoredObject(response,length==null?0L:length,response.response().contentType());
  }catch(SdkException exception){throw unavailable(exception);}
 }
 public void delete(String key){
  try{BucketObject object=bucketObject(key);client.deleteObject(DeleteObjectRequest.builder().bucket(object.bucket()).key(object.key()).build());}
  catch(SdkException exception){throw unavailable(exception);}
 }
 private BucketObject bucketObject(String key){
  if(key==null||key.isBlank())throw new MediaException("INVALID_STORAGE_KEY","Media storage key is required");
  if(key.startsWith("public/"))return new BucketObject(properties.getPublicBucketName(),key.substring("public/".length()));
  if(key.startsWith("private/"))return new BucketObject(properties.getPrivateBucketName(),key.substring("private/".length()));
  throw new MediaException("INVALID_STORAGE_KEY","R2 media keys must start with public/ or private/");
 }
 private record BucketObject(String bucket,String key){}
 private MediaException unavailable(SdkException cause){log.warn("R2 object storage request failed: {}",cause.getMessage());return new MediaException("STORAGE_UNAVAILABLE","Media storage is temporarily unavailable",cause);}
}
