package com.yeyamo_mobile.api.culture_service.domain;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "culture_recipe_details")
public class RecipeDetailsEntity {
    @Id
    @Column(name = "content_id")
    private UUID contentId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "content_id")
    private CultureContent content;

    @ElementCollection
    @CollectionTable(name = "culture_recipe_ingredients", joinColumns = @JoinColumn(name = "content_id"))
    @OrderColumn(name = "display_order")
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "culture_recipe_steps", joinColumns = @JoinColumn(name = "content_id"))
    @OrderColumn(name = "display_order")
    private List<RecipeStep> steps = new ArrayList<>();

    @Column(name = "prep_time_minutes")
    private Integer prepTimeMinutes;

    private Integer servings;

    protected RecipeDetailsEntity() { }

    public static RecipeDetailsEntity create(CultureContent content, List<RecipeIngredient> ingredients,
            List<RecipeStep> steps, Integer prepTimeMinutes, Integer servings) {
        RecipeDetailsEntity details = new RecipeDetailsEntity();
        details.content = content;
        details.ingredients = ingredients == null ? new ArrayList<>() : new ArrayList<>(ingredients);
        details.steps = steps == null ? new ArrayList<>() : new ArrayList<>(steps);
        details.prepTimeMinutes = prepTimeMinutes;
        details.servings = servings;
        return details;
    }

    public UUID getContentId() { return contentId; }
    public List<RecipeIngredient> getIngredients() { return List.copyOf(ingredients); }
    public List<RecipeStep> getSteps() { return List.copyOf(steps); }
    public Integer getPrepTimeMinutes() { return prepTimeMinutes; }
    public Integer getServings() { return servings; }

    @Embeddable
    public static class RecipeIngredient {
        @Column(nullable = false, length = 255)
        private String name;
        @Column(length = 80)
        private String quantity;
        @Column(length = 80)
        private String unit;

        protected RecipeIngredient() { }
        public RecipeIngredient(String name, String quantity, String unit) {
            this.name = name.trim();
            this.quantity = blankToNull(quantity);
            this.unit = blankToNull(unit);
        }
        public String getName() { return name; }
        public String getQuantity() { return quantity; }
        public String getUnit() { return unit; }
    }

    @Embeddable
    public static class RecipeStep {
        @Column(nullable = false, columnDefinition = "TEXT")
        private String instruction;

        protected RecipeStep() { }
        public RecipeStep(String instruction) { this.instruction = instruction.trim(); }
        public String getInstruction() { return instruction; }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
