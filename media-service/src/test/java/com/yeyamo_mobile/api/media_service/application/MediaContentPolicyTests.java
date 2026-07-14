package com.yeyamo_mobile.api.media_service.application;
import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;import com.yeyamo_mobile.api.media_service.domain.model.MediaType;
class MediaContentPolicyTests{
 private final MediaContentPolicy policy=new MediaContentPolicy(1000,2000);
 @Test void validatesMagicAndMime(){byte[] jpeg={(byte)0xff,(byte)0xd8,0,0,0,0,0,0,0,0,0,0,0};assertEquals(MediaType.IMAGE,policy.validate("image/jpeg",jpeg));}
 @Test void rejectsSpoofedImage(){assertThrows(MediaException.class,()->policy.validate("image/png","not-an-image".getBytes()));}
 @Test void validatesMp4Signature(){byte[] mp4={0,0,0,20,'f','t','y','p','i','s','o','m',0};assertEquals(MediaType.VIDEO,policy.validate("video/mp4",mp4));}
}
