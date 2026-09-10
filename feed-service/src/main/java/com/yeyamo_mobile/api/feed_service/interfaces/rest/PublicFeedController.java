package com.yeyamo_mobile.api.feed_service.interfaces.rest;

import com.yeyamo_mobile.api.feed_service.application.FeedQueryService;
import com.yeyamo_mobile.api.feed_service.application.PublicFeedPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/feed")
@Tag(name = "Public feed")
public class PublicFeedController {
    private final FeedQueryService service;
    public PublicFeedController(FeedQueryService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Get the public, non-personalized feed")
    public PublicFeedPage feed(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.publicFeed(page, size);
    }

    @GetMapping("/users/{userId}/posts")
    @Operation(summary = "Get public posts by author")
    public PublicFeedPage postsByAuthor(@PathVariable String userId, @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.publicFeedByAuthor(userId, page, size);
    }
}
