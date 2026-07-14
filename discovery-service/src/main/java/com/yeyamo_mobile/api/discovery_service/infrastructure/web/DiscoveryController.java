package com.yeyamo_mobile.api.discovery_service.infrastructure.web;

import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.discovery_service.application.*;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController @RequestMapping("/api/v1/discovery")
public class DiscoveryController {
    private final DiscoveryQueryService queries;
    public DiscoveryController(DiscoveryQueryService queries){this.queries=queries;}
    @GetMapping("/search") @Operation(summary="Search discoverable places and content",security=@SecurityRequirement(name="bearerAuth"))
    public DiscoveryPage search(@RequestParam(required=false,name="q") String query,@RequestParam(required=false) DiscoveryType type,
            @RequestParam(required=false) String categoryCode,@RequestParam(required=false) String regionCode,
            @RequestParam(required=false,name="lat") Double latitude,@RequestParam(required=false,name="lng") Double longitude,
            @RequestParam(required=false) Double radiusKm,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return queries.search(new DiscoverySearch(query,type,categoryCode,regionCode,latitude,longitude,radiusKm,page,size,false));
    }
    @GetMapping("/trending") @Operation(summary="List trending places and content",security=@SecurityRequirement(name="bearerAuth"))
    public DiscoveryPage trending(@RequestParam(required=false) DiscoveryType type,@RequestParam(required=false) String regionCode,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return queries.search(new DiscoverySearch(null,type,null,regionCode,null,null,null,page,size,true));
    }
}
