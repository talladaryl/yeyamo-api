package com.yeyamo_mobile.api.gamification_service.infrastructure.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.gamification_service.application.BadgeCatalogEntry;
import com.yeyamo_mobile.api.gamification_service.application.BadgeStats;
import com.yeyamo_mobile.api.gamification_service.application.GamificationService;
import com.yeyamo_mobile.api.gamification_service.application.LeaderboardEntry;
import com.yeyamo_mobile.api.gamification_service.domain.Badge;
import com.yeyamo_mobile.api.gamification_service.domain.PassportStamp;
import com.yeyamo_mobile.api.gamification_service.domain.Progress;
import com.yeyamo_mobile.api.gamification_service.domain.Reward;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/me")
public class GamificationController {
    private final GamificationService service;

    public GamificationController(GamificationService service) {
        this.service = service;
    }

    @GetMapping("/xp")
    @Operation(summary = "Read my XP and level", security = @SecurityRequirement(name = "bearerAuth"))
    public Progress xp(Authentication authentication) {
        return service.view(authentication.getName()).progress();
    }

    @GetMapping("/badges")
    public List<Badge> badges(Authentication authentication) {
        return service.view(authentication.getName()).badges();
    }

    @GetMapping("/badges/catalog")
    public List<BadgeCatalogEntry> badgeCatalog(Authentication authentication) {
        return service.catalog(authentication.getName());
    }

    @GetMapping("/badges/catalog/{code}")
    public BadgeCatalogEntry badge(Authentication authentication, @PathVariable String code) {
        return service.badge(authentication.getName(), code);
    }

    @GetMapping("/badges/stats")
    public BadgeStats badgeStats(Authentication authentication) {
        return service.stats(authentication.getName());
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntry> leaderboard(
            @RequestParam(defaultValue = "50") int limit) {
        return service.leaderboard(Math.max(1, Math.min(100, limit)));
    }

    @GetMapping("/passport")
    public List<PassportStamp> passport(Authentication authentication) {
        return service.view(authentication.getName()).passport();
    }

    @GetMapping("/streaks")
    public StreakResponse streaks(Authentication authentication) {
        Progress progress = service.view(authentication.getName()).progress();
        return new StreakResponse(
                progress.currentStreak(), progress.longestStreak(), progress.lastActivityDate());
    }

    @GetMapping("/rewards")
    public List<Reward> rewards(Authentication authentication) {
        return service.view(authentication.getName()).rewards();
    }

    @PostMapping("/rewards/{id}/claim")
    public Reward claim(Authentication authentication, @PathVariable UUID id,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlation) {
        return service.claim(authentication.getName(), id, correlation);
    }

    public record StreakResponse(int current, int longest, LocalDate lastActivityDate) {
    }
}
