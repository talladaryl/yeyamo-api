package com.yeyamo_mobile.api.user_service.infrastructure.client;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.yeyamo_mobile.api.user_service.application.exception.UserProfileException;
import org.springframework.http.HttpStatus;

@Component
public class CategoryValidationClient {
    private final RestClient client;
    public CategoryValidationClient(RestClient.Builder builder,
            @Value("${yeyamo.services.place.url:http://place-service:8080}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }
    public void validate(Set<String> requested) {
        if (requested == null || requested.isEmpty()) return;
        Category[] roots;
        try { roots = client.get().uri("/api/v1/categories").retrieve().body(Category[].class); }
        catch (RuntimeException exception) {
            throw new UserProfileException("CATEGORY_VALIDATION_UNAVAILABLE", "Validation des catégories indisponible", HttpStatus.SERVICE_UNAVAILABLE);
        }
        Set<String> valid = new HashSet<>();
        if (roots != null) Arrays.stream(roots).forEach(category -> collect(category, valid));
        if (!valid.containsAll(requested)) {
            throw new UserProfileException("INVALID_INTEREST_CATEGORY", "Une catégorie sélectionnée est inconnue ou inactive", HttpStatus.BAD_REQUEST);
        }
    }
    private void collect(Category category, Set<String> values) {
        if (category.active() && category.slug() != null) values.add(category.slug());
        if (category.children() != null) category.children().forEach(child -> collect(child, values));
    }
    public record Category(String slug, boolean active, List<Category> children) {}
}
