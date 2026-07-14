package com.yeyamo_mobile.api.interaction_service.domain.port;
import java.util.UUID;import com.yeyamo_mobile.api.interaction_service.domain.model.PostShare;
public interface ShareRepository{PostShare save(PostShare share);java.util.Optional<PostShare> findById(UUID id);long countByPost(UUID postId);}
