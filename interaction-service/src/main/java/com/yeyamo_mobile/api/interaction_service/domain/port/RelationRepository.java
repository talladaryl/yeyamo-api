package com.yeyamo_mobile.api.interaction_service.domain.port;
import java.util.*;import com.yeyamo_mobile.api.interaction_service.domain.model.*;
public interface RelationRepository{
 Optional<PostRelation> find(UUID postId,String userId,RelationType type);PostRelation save(PostRelation relation);void delete(PostRelation relation);
 long count(UUID postId,RelationType type);List<PostRelation> findFavorites(String userId,int limit);
}
