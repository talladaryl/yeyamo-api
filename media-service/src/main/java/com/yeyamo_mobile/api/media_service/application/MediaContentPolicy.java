package com.yeyamo_mobile.api.media_service.application;
import java.util.*;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Component;
import com.yeyamo_mobile.api.media_service.domain.model.MediaType;
@Component
public class MediaContentPolicy{
 private static final Set<String> IMAGES=Set.of("image/jpeg","image/png","image/webp");private static final Set<String> VIDEOS=Set.of("video/mp4","video/webm","video/quicktime");
 private final long maxImage,maxVideo;public MediaContentPolicy(@Value("${media.upload.max-image-bytes:10485760}")long image,@Value("${media.upload.max-video-bytes:104857600}")long video){maxImage=image;maxVideo=video;}
 public MediaType validate(String contentType,byte[] bytes){String type=contentType==null?"":contentType.toLowerCase(Locale.ROOT);MediaType mediaType=IMAGES.contains(type)?MediaType.IMAGE:VIDEOS.contains(type)?MediaType.VIDEO:null;
  if(mediaType==null)throw new MediaException("UNSUPPORTED_MEDIA_TYPE","Supported types: JPEG, PNG, WEBP, MP4, WEBM, MOV");
  long max=mediaType==MediaType.IMAGE?maxImage:maxVideo;if(bytes.length==0||bytes.length>max)throw new MediaException("INVALID_MEDIA_SIZE","Media size is invalid");
  if(mediaType==MediaType.IMAGE&&!imageMagic(bytes))throw new MediaException("INVALID_MEDIA_CONTENT","Image signature does not match content type");
  if(mediaType==MediaType.VIDEO&&!videoMagic(bytes))throw new MediaException("INVALID_MEDIA_CONTENT","Video signature is not recognized");return mediaType;}
 private boolean imageMagic(byte[] b){return b.length>12&&((b[0]&255)==0xff&&(b[1]&255)==0xd8||(b[0]&255)==0x89&&b[1]=='P'&&b[2]=='N'&&b[3]=='G'||b[0]=='R'&&b[1]=='I'&&b[2]=='F'&&b[3]=='F'&&b[8]=='W'&&b[9]=='E'&&b[10]=='B'&&b[11]=='P');}
 private boolean videoMagic(byte[] b){return b.length>12&&(b[4]=='f'&&b[5]=='t'&&b[6]=='y'&&b[7]=='p'||(b[0]&255)==0x1a&&(b[1]&255)==0x45&&(b[2]&255)==0xdf&&(b[3]&255)==0xa3);}
}
