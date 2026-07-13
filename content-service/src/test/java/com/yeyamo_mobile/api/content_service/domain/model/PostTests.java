package com.yeyamo_mobile.api.content_service.domain.model;
import static org.junit.jupiter.api.Assertions.*;import java.util.*;import org.junit.jupiter.api.Test;
class PostTests{
 @Test void normalizesReferencesAndPublishes(){UUID media=UUID.randomUUID();Post p=Post.draft("user-1"," Voyage ",PostVisibility.PUBLIC,null,List.of(media,media),Set.of("#Cameroun","Nature"));
  assertEquals(1,p.getMediaIds().size());assertTrue(p.getHashtags().contains("cameroun"));p.publish();assertEquals(PostStatus.PUBLISHED,p.getStatus());assertNotNull(p.getPublishedAt());}
 @Test void refusesEmptyPublication(){Post p=Post.draft("u",null,null,null,List.of(),Set.of());assertThrows(IllegalStateException.class,p::publish);}
 @Test void enforcesLifecycle(){Post p=Post.draft("u","hello",null,null,null,null);assertThrows(IllegalStateException.class,p::archive);p.publish();p.archive();assertEquals(PostStatus.ARCHIVED,p.getStatus());}
 @Test void softDeletesIdempotently(){Post p=Post.draft("u","hello",null,null,null,null);p.delete();p.delete();assertEquals(PostStatus.DELETED,p.getStatus());assertNotNull(p.getDeletedAt());}
}
