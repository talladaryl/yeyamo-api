package com.yeyamo_mobile.api.user_service.interfaces.rest;

import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.user_service.application.UserProfileService;

@RestController
@RequestMapping("/internal/users")
public class InternalUserActivityController {
    private final UserProfileService service;
    public InternalUserActivityController(UserProfileService service) { this.service = service; }
    @GetMapping("/inactive")
    public List<String> inactive(@RequestParam Instant before, @RequestParam(defaultValue = "500") int limit) {
        return service.inactiveBefore(before, limit);
    }
}
