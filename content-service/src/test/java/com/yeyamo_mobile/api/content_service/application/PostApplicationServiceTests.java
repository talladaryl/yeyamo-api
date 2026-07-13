package com.yeyamo_mobile.api.content_service.application;
import static org.junit.jupiter.api.Assertions.*;import java.util.*;import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.content_service.domain.model.*;import com.yeyamo_mobile.api.content_service.domain.port.PostRepository;
class PostApplicationServiceTests{
 @Test void createsAndPublishesWithOutbox(){MemoryPosts repo=new MemoryPosts();List<String> events=new ArrayList<>();PostApplicationService service=new PostApplicationService(repo,(e,p,c,a)->events.add(e));
  Post post=service.createDraft("author",new PostCommand("Hello",PostVisibility.PUBLIC,null,List.of(),Set.of("travel")),"c");service.publish(post.getId(),"author",false,"c");
  assertEquals(List.of("content.post.created","content.post.published"),events);assertEquals(PostStatus.PUBLISHED,repo.data.get(post.getId()).getStatus());}
 @Test void protectsOwnership(){MemoryPosts repo=new MemoryPosts();PostApplicationService service=new PostApplicationService(repo,(a,b,c,d)->{});Post post=service.createDraft("owner",new PostCommand("x",null,null,null,null),null);
  assertThrows(ContentException.class,()->service.publish(post.getId(),"intruder",false,null));service.publish(post.getId(),"admin",true,null);}
 @Test void hidesPrivateAndDraftPosts(){MemoryPosts repo=new MemoryPosts();PostApplicationService service=new PostApplicationService(repo,(a,b,c,d)->{});Post post=service.createDraft("owner",new PostCommand("x",PostVisibility.PRIVATE,null,null,null),null);
  assertThrows(ContentException.class,()->service.publicPost(post.getId()));}
 static class MemoryPosts implements PostRepository{
  final Map<UUID,Post>data=new HashMap<>();public Post save(Post p){data.put(p.getId(),p);return p;}public Optional<Post>findById(UUID id){return Optional.ofNullable(data.get(id));}
  public List<Post>findByAuthor(String a,int l){return data.values().stream().filter(p->p.getAuthorId().equals(a)).toList();}
  public List<Post>findPublishedByHashtag(String h,int l){return data.values().stream().filter(p->p.getHashtags().contains(h)&&p.publiclyVisible()).toList();}
  public List<Post>findPublishedByCatalogAsset(UUID id,int l){return data.values().stream().filter(p->id.equals(p.getCatalogAssetId())&&p.publiclyVisible()).toList();}
 }
}
