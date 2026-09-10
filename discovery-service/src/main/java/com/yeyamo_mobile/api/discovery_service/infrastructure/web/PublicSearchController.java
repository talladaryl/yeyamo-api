package com.yeyamo_mobile.api.discovery_service.infrastructure.web;

import java.time.Instant;
import java.util.List;
import com.yeyamo_mobile.api.discovery_service.application.DiscoveryPage;
import com.yeyamo_mobile.api.discovery_service.application.DiscoveryQueryService;
import com.yeyamo_mobile.api.discovery_service.application.DiscoveryScope;
import com.yeyamo_mobile.api.discovery_service.application.DiscoverySearch;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryDocument;
import com.yeyamo_mobile.api.discovery_service.domain.model.DiscoveryType;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/public/search")
public class PublicSearchController {
    private final DiscoveryQueryService queries;
    public PublicSearchController(DiscoveryQueryService queries) { this.queries = queries; }

    @GetMapping
    @Operation(summary = "Search public discoverable content")
    public PublicSearchPage search(@RequestParam("q") @Size(min = 2, max = 120) String query,
            @RequestParam(required = false) DiscoveryType type,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String regionCode,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        DiscoveryPage result = queries.search(new DiscoverySearch(query.trim(), type, categoryCode, regionCode, null, null, null,
                page, size, false, null, null, null, null, null, null, null, null, null, java.util.Set.of(), DiscoveryScope.AFRICA));
        List<PublicSearchItem> items = result.items().stream()
                .filter(document -> document.active() && "PUBLIC".equals(document.launchVisibility()))
                .map(PublicSearchItem::from)
                .toList();
        return new PublicSearchPage(result.page(), result.size(), result.hasNext(), items, result.generatedAt());
    }

    public record PublicSearchPage(int page, int size, boolean hasNext, List<PublicSearchItem> items, Instant generatedAt) { }
    public record PublicSearchItem(String type, String id, String title, String subtitle, String categoryCode,
            String regionCode, String city) {
        static PublicSearchItem from(DiscoveryDocument document) {
            return new PublicSearchItem(document.type().name(), document.id().toString(), document.title(),
                    document.description(), document.categoryCode(), document.regionCode(), document.city());
        }
    }
}
