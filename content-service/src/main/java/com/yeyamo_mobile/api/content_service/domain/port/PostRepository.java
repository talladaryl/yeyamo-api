package com.yeyamo_mobile.api.content_service.domain.port;
import java.util.*;import com.yeyamo_mobile.api.content_service.domain.model.Post;
public interface PostRepository{
 Post save(Post post);Optional<Post> findById(UUID id);List<Post> findByAuthor(String authorId,int limit);
 List<Post> findPublishedByHashtag(String hashtag,int limit);List<Post> findPublishedByCatalogAsset(UUID catalogAssetId,int limit);
}
