package com.yeyamo_mobile.api.interaction_service.interfaces.rest;
import java.util.*;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;import com.yeyamo_mobile.api.interaction_service.application.InteractionQueryService;
import io.swagger.v3.oas.annotations.*;import io.swagger.v3.oas.annotations.security.SecurityRequirement;import io.swagger.v3.oas.annotations.tags.Tag;import jakarta.validation.constraints.*;
@RestController @RequestMapping("/api/v1/saves")@Tag(name="Saved posts")
public class SavedPostController{private final InteractionQueryService queries;public SavedPostController(InteractionQueryService q){queries=q;}
 @GetMapping@Operation(summary="List my saved posts",security=@SecurityRequirement(name="bearerAuth"))
 public List<FavoriteResponse> mine(@RequestParam(defaultValue="50")@Min(1)@Max(100)int limit,Authentication auth){return queries.favorites(auth.getName(),limit).stream().map(FavoriteResponse::from).toList();}}
