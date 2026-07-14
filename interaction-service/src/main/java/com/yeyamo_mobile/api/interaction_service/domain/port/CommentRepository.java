package com.yeyamo_mobile.api.interaction_service.domain.port;
import java.util.*;import com.yeyamo_mobile.api.interaction_service.domain.model.Comment;
public interface CommentRepository{Comment save(Comment comment);Optional<Comment> findById(UUID id);List<Comment> findActiveByPost(UUID postId,int limit);long countActiveByPost(UUID postId);}
