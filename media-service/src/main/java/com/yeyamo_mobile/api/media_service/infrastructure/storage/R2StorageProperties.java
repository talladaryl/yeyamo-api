package com.yeyamo_mobile.api.media_service.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** R2 credentials are supplied only through environment variables. */
@ConfigurationProperties(prefix = "r2")
public class R2StorageProperties {
 private String accountId;
 private String accessKeyId;
 private String secretAccessKey;
 private String bucketName;
 private String publicBaseUrl;
 private String endpointOverride;

 public String getAccountId(){return accountId;} public void setAccountId(String value){accountId=value;}
 public String getAccessKeyId(){return accessKeyId;} public void setAccessKeyId(String value){accessKeyId=value;}
 public String getSecretAccessKey(){return secretAccessKey;} public void setSecretAccessKey(String value){secretAccessKey=value;}
 public String getBucketName(){return bucketName;} public void setBucketName(String value){bucketName=value;}
 public String getPublicBaseUrl(){return publicBaseUrl;} public void setPublicBaseUrl(String value){publicBaseUrl=value;}
 public String getEndpointOverride(){return endpointOverride;} public void setEndpointOverride(String value){endpointOverride=value;}
}
