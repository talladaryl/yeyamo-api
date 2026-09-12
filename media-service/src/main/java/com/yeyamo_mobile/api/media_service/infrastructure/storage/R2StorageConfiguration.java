package com.yeyamo_mobile.api.media_service.infrastructure.storage;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@ConditionalOnProperty(prefix = "r2", name = "enabled", havingValue = "true", matchIfMissing = true)
public class R2StorageConfiguration {
 @Bean
 S3Client r2S3Client(R2StorageProperties properties){
  required(properties.getAccountId(),"R2_ACCOUNT_ID");
  required(properties.getAccessKeyId(),"R2_ACCESS_KEY_ID");
  required(properties.getSecretAccessKey(),"R2_SECRET_ACCESS_KEY");
  required(properties.getPublicBucketName(),"R2_PUBLIC_BUCKET_NAME");
  required(properties.getPrivateBucketName(),"R2_PRIVATE_BUCKET_NAME");
  String endpoint=properties.getEndpointOverride();
  if(endpoint==null||endpoint.isBlank())endpoint="https://"+properties.getAccountId()+".r2.cloudflarestorage.com";
  return S3Client.builder()
   .endpointOverride(URI.create(endpoint))
   .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(properties.getAccessKeyId(),properties.getSecretAccessKey())))
   .region(Region.of("auto"))
   .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
   .build();
 }
 private static void required(String value,String variable){if(value==null||value.isBlank())throw new IllegalStateException(variable+" must be configured");}
}
