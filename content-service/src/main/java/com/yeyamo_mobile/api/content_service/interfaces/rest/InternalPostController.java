package com.yeyamo_mobile.api.content_service.interfaces.rest;

import com.yeyamo_mobile.api.content_service.application.PostApplicationService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/posts")
public class InternalPostController {
    private final PostApplicationService posts;
    public InternalPostController(PostApplicationService posts) { this.posts = posts; }
    @GetMapping("/{postId}/owner")
    public PostOwnerResponse owner(@PathVariable UUID postId) {
        var post = posts.internalPost(postId);
        return new PostOwnerResponse(post.getId(), post.getAuthorId());
    }
    public record PostOwnerResponse(UUID postId, String ownerAuthUserId) {}
}
