package com.yeyamo_mobile.api.content_service.application;
import static org.junit.jupiter.api.Assertions.*;import java.util.*;import org.junit.jupiter.api.Test;
import com.yeyamo_mobile.api.content_service.domain.model.*;import com.yeyamo_mobile.api.content_service.domain.port.PostRepository;
import com.yeyamo_mobile.api.content_service.infrastructure.outbox.ContentOutboxPort;
class PostApplicationServiceTests{
 @Test void createsAndPublishesWithOutbox(){MemoryPosts repo=new MemoryPosts();List<String> events=new ArrayList<>();
  ContentOutboxPort outbox=new ContentOutboxPort(){
   public void append(String e,Post p,String c,String a){events.add(e);}
   public void append(String e,String agg,String act,String c,Map<String,String> pay){}
  };
  PostApplicationService service=new PostApplicationService(repo,outbox,(type,id)->{});
  Post post=service.createDraft("author",new PostCommand("Hello",PostVisibility.PUBLIC,null,List.of(),Set.of("travel")),"c");service.publish(post.getId(),"author",false,"c");
  assertEquals(List.of("content.post.created","content.post.published"),events);assertEquals(PostStatus.PUBLISHED,repo.data.get(post.getId()).getStatus());}
 @Test void protectsOwnership(){MemoryPosts repo=new MemoryPosts();
  ContentOutboxPort outbox=new ContentOutboxPort(){
   public void append(String e,Post p,String c,String a){}
   public void append(String e,String agg,String act,String c,Map<String,String> pay){}
  };
  PostApplicationService service=new PostApplicationService(repo,outbox,(type,id)->{});Post post=service.createDraft("owner",new PostCommand("x",null,null,null,null),null);
  assertThrows(ContentException.class,()->service.publish(post.getId(),"intruder",false,null));service.publish(post.getId(),"admin",true,null);}
 @Test void hidesPrivateAndDraftPosts(){MemoryPosts repo=new MemoryPosts();
  ContentOutboxPort outbox=new ContentOutboxPort(){
   public void append(String e,Post p,String c,String a){}
   public void append(String e,String agg,String act,String c,Map<String,String> pay){}
  };
  PostApplicationService service=new PostApplicationService(repo,outbox,(type,id)->{});Post post=service.createDraft("owner",new PostCommand("x",PostVisibility.PRIVATE,null,null,null),null);
  assertThrows(ContentException.class,()->service.publicPost(post.getId()));}
 @Test void returnsOnlyPublishedPublicPostsForProfileAndLikes(){MemoryPosts repo=new MemoryPosts();ContentOutboxPort outbox=new ContentOutboxPort(){public void append(String e,Post p,String c,String a){}public void append(String e,String agg,String act,String c,Map<String,String> pay){}};
  PostApplicationService service=new PostApplicationService(repo,outbox,(type,id)->{});
  Post visible=service.createDraft("author",new PostCommand("public",PostVisibility.PUBLIC,null,null,null),null);service.publish(visible.getId(),"author",false,null);
  Post hidden=service.createDraft("author",new PostCommand("private",PostVisibility.PRIVATE,null,null,null),null);service.publish(hidden.getId(),"author",false,null);
  assertEquals(List.of(visible.getId()),service.publicPostsByAuthor("author",50).stream().map(Post::getId).toList());
  assertEquals(List.of(visible.getId()),service.publicPostsByIds(List.of(hidden.getId(),visible.getId())).stream().map(Post::getId).toList());}
 static class MemoryPosts implements PostRepository{
  final Map<UUID,Post>data=new HashMap<>();public Post save(Post p){data.put(p.getId(),p);return p;}public Optional<Post>findById(UUID id){return Optional.ofNullable(data.get(id));}
 public List<Post>findByAuthor(String a,int l){return data.values().stream().filter(p->p.getAuthorId().equals(a)).toList();}
  public List<Post>findPublishedByIds(List<UUID> ids){return ids.stream().map(data::get).filter(Objects::nonNull).filter(Post::publiclyVisible).toList();}
  public List<Post>findPublishedByAuthor(String authorId,int limit){return data.values().stream().filter(post->post.getAuthorId().equals(authorId)&&post.publiclyVisible()).limit(limit).toList();}
  public List<Post>findPublishedByHashtag(String h,int l){return data.values().stream().filter(p->p.getHashtags().contains(h)&&p.publiclyVisible()).toList();}
  public List<Post>findPublishedByCatalogAsset(UUID id,int l){return data.values().stream().filter(p->id.equals(p.getCatalogAssetId())&&p.publiclyVisible()).toList();}
 }
}
