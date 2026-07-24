package com.yeyamo_mobile.api.user_service.interfaces.rest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.user_service.application.SocialGraphService;
import com.yeyamo_mobile.api.user_service.domain.model.UserProfile;
import com.yeyamo_mobile.api.user_service.interfaces.rest.dto.NetworkActivityResponse;
import com.yeyamo_mobile.api.user_service.interfaces.rest.dto.SocialStatsResponse;
import com.yeyamo_mobile.api.user_service.interfaces.rest.dto.SocialSettingsRequest;
import com.yeyamo_mobile.api.user_service.interfaces.rest.dto.SocialSettingsResponse;
import com.yeyamo_mobile.api.user_service.interfaces.rest.dto.UserProfileSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users/social")
@Tag(name = "Social Graph", description = "Follow, block, and social discovery endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SocialGraphController {
    
    private final SocialGraphService socialGraphService;

    public SocialGraphController(SocialGraphService socialGraphService) {
        this.socialGraphService = socialGraphService;
    }

    // ─── FOLLOW OPERATIONS ──────────────────────────────────────────────────────

    @PostMapping("/{userId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Follow a user")
    public void follow(
            @PathVariable UUID userId,
            Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        socialGraphService.follow(authentication.getName(), userId, correlationId);
    }

    @DeleteMapping("/{userId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unfollow a user")
    public void unfollow(
            @PathVariable UUID userId,
            Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        socialGraphService.unfollow(authentication.getName(), userId, correlationId);
    }

    @GetMapping("/following")
    @Operation(summary = "Get list of users I'm following")
    public Page<UserProfileSummaryResponse> getFollowing(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        String authUserId = authentication.getName();
        Page<UserProfile> following = socialGraphService.getFollowing(authUserId, pageable);
        
        return following.map(profile -> {
            long followersCount = socialGraphService.countFollowers(profile.getId());
            long followingCount = socialGraphService.countFollowing(profile.getId());
            boolean isFollowing = true; // Already in following list
            
            return UserProfileSummaryResponse.from(profile, isFollowing, followersCount, followingCount);
        });
    }

    @GetMapping("/followers")
    @Operation(summary = "Get list of my followers")
    public Page<UserProfileSummaryResponse> getFollowers(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        String authUserId = authentication.getName();
        Page<UserProfile> followers = socialGraphService.getFollowers(authUserId, pageable);
        
        return followers.map(profile -> {
            long followersCount = socialGraphService.countFollowers(profile.getId());
            long followingCount = socialGraphService.countFollowing(profile.getId());
            boolean isFollowing = socialGraphService.isFollowing(authUserId, profile.getId());
            
            return UserProfileSummaryResponse.from(profile, isFollowing, followersCount, followingCount);
        });
    }

    @GetMapping("/{userId}/following")
    @Operation(summary = "Get list of users that a specific user is following")
    public Page<UserProfileSummaryResponse> getUserFollowing(
            @PathVariable UUID userId,
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        String authUserId = authentication.getName();
        Page<UserProfile> following = socialGraphService.getFollowing(userId, pageable);
        
        return following.map(profile -> {
            long followersCount = socialGraphService.countFollowers(profile.getId());
            long followingCount = socialGraphService.countFollowing(profile.getId());
            boolean isFollowing = socialGraphService.isFollowing(authUserId, profile.getId());
            
            return UserProfileSummaryResponse.from(profile, isFollowing, followersCount, followingCount);
        });
    }

    @GetMapping("/{userId}/followers")
    @Operation(summary = "Get list of followers of a specific user")
    public Page<UserProfileSummaryResponse> getUserFollowers(
            @PathVariable UUID userId,
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        String authUserId = authentication.getName();
        Page<UserProfile> followers = socialGraphService.getFollowers(userId, pageable);
        
        return followers.map(profile -> {
            long followersCount = socialGraphService.countFollowers(profile.getId());
            long followingCount = socialGraphService.countFollowing(profile.getId());
            boolean isFollowing = socialGraphService.isFollowing(authUserId, profile.getId());
            
            return UserProfileSummaryResponse.from(profile, isFollowing, followersCount, followingCount);
        });
    }

    @GetMapping("/stats")
    @Operation(summary = "Get my social stats")
    public SocialStatsResponse getMyStats(Authentication authentication) {
        String authUserId = authentication.getName();
        long followersCount = socialGraphService.countFollowers(authUserId);
        long followingCount = socialGraphService.countFollowing(authUserId);
        
        return new SocialStatsResponse(followersCount, followingCount);
    }

    @GetMapping("/{userId}/stats")
    @Operation(summary = "Get social stats of a specific user")
    public SocialStatsResponse getUserStats(@PathVariable UUID userId) {
        long followersCount = socialGraphService.countFollowers(userId);
        long followingCount = socialGraphService.countFollowing(userId);
        
        return new SocialStatsResponse(followersCount, followingCount);
    }

    // ─── BLOCK OPERATIONS ───────────────────────────────────────────────────────

    @PostMapping("/{userId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Block a user")
    public void block(
            @PathVariable UUID userId,
            Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        socialGraphService.block(authentication.getName(), userId, correlationId);
    }

    @DeleteMapping("/{userId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unblock a user")
    public void unblock(
            @PathVariable UUID userId,
            Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        socialGraphService.unblock(authentication.getName(), userId, correlationId);
    }

    // ─── SUGGESTIONS ────────────────────────────────────────────────────────────

    @GetMapping("/blocked")
    @Operation(summary = "Get the profiles I blocked")
    public List<UserProfileSummaryResponse> getBlockedUsers(Authentication authentication) {
        return socialGraphService.getBlockedUsers(authentication.getName()).stream()
                .map(UserProfileSummaryResponse::fromBasic)
                .toList();
    }

    @DeleteMapping("/followers/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove one of my followers")
    public void removeFollower(
            @PathVariable UUID userId,
            Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        socialGraphService.removeFollower(authentication.getName(), userId, correlationId);
    }

    @GetMapping("/settings")
    @Operation(summary = "Get my social privacy and notification settings")
    public SocialSettingsResponse getSettings(Authentication authentication) {
        return SocialSettingsResponse.from(socialGraphService.getSocialSettings(authentication.getName()));
    }

    @PutMapping("/settings")
    @Operation(summary = "Partially update my social privacy and notification settings")
    public SocialSettingsResponse updateSettings(
            @Valid @RequestBody SocialSettingsRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        var privacy = request.privacy();
        var notifications = request.notifications();
        var preferences = request.preferences();
        var update = new SocialGraphService.SocialSettingsUpdate(
                privacy == null ? null : privacy.profileVisibility(),
                privacy == null ? null : privacy.showActivity(),
                privacy == null ? null : privacy.showFollowers(),
                privacy == null ? null : privacy.showFollowing(),
                notifications == null ? null : notifications.newFollowers(),
                notifications == null ? null : notifications.followRequests(),
                notifications == null ? null : notifications.mentions(),
                notifications == null ? null : notifications.activityUpdates(),
                preferences == null ? null : preferences.allowSuggestions(),
                preferences == null ? null : preferences.allowMessagesFromStrangers());
        return SocialSettingsResponse.from(socialGraphService.updateSocialSettings(
                authentication.getName(), update, correlationId));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get friend suggestions (friends of friends)")
    public List<UserProfileSummaryResponse> getSuggestions(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit) {
        
        String authUserId = authentication.getName();
        List<UserProfile> suggestions = socialGraphService.getSuggestions(authUserId, Math.min(limit, 50));
        
        return suggestions.stream()
                .map(profile -> {
                    long followersCount = socialGraphService.countFollowers(profile.getId());
                    long followingCount = socialGraphService.countFollowing(profile.getId());
                    
                    return UserProfileSummaryResponse.from(profile, false, followersCount, followingCount);
                })
                .collect(Collectors.toList());
    }

    // ─── SEARCH ─────────────────────────────────────────────────────────────────

    @GetMapping("/search")
    @Operation(summary = "Search users (excluding blocked)")
    public Page<UserProfileSummaryResponse> searchUsers(
            @RequestParam String query,
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        
        String authUserId = authentication.getName();
        Page<UserProfile> results = socialGraphService.searchUsers(query, pageable, authUserId);
        
        return results.map(profile -> {
            if (profile == null) return null; // Filtered blocked user
            
            long followersCount = socialGraphService.countFollowers(profile.getId());
            long followingCount = socialGraphService.countFollowing(profile.getId());
            boolean isFollowing = socialGraphService.isFollowing(authUserId, profile.getId());
            
            return UserProfileSummaryResponse.from(profile, isFollowing, followersCount, followingCount);
        });
    }

    // ─── ACTIVITY ───────────────────────────────────────────────────────────────

    @GetMapping("/activity")
    @Operation(summary = "Get network activity (recent follows from people I follow)")
    public List<NetworkActivityResponse> getNetworkActivity(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit) {
        
        String authUserId = authentication.getName();
        var activities = socialGraphService.getNetworkActivity(authUserId, Math.min(limit, 100));
        
        return activities.stream()
                .map(NetworkActivityResponse::from)
                .collect(Collectors.toList());
    }
}
