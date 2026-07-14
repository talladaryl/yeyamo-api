package com.yeyamo_mobile.api.media_service.application.thumbnail;
import com.yeyamo_mobile.api.media_service.domain.model.MediaType;
public interface ThumbnailStrategy{
 boolean supports(MediaType type);
 Thumbnail generate(byte[] original,String contentType);
 record Thumbnail(byte[] bytes,String contentType,Integer width,Integer height,Long durationMs){}
}
