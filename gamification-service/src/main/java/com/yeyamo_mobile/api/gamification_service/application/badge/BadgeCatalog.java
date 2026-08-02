package com.yeyamo_mobile.api.gamification_service.application.badge;

import java.util.List;
import java.util.NoSuchElementException;

import com.yeyamo_mobile.api.gamification_service.domain.BadgeDefinition;

public final class BadgeCatalog {
    private static final List<BadgeDefinition> DEFINITIONS = List.of(
            new BadgeDefinition("FIRST_POST", "Premier récit", "Publier un premier contenu"),
            new BadgeDefinition("EXPLORER", "Explorateur", "Effectuer un premier check-in"),
            new BadgeDefinition("TRAVELER_5", "Voyageur", "Visiter cinq destinations"),
            new BadgeDefinition("SOCIAL_10", "Sociable", "Réaliser dix interactions"),
            new BadgeDefinition("FIRST_BOOKING", "Premier voyage réservé", "Confirmer une première réservation"),
            new BadgeDefinition("STREAK_7", "Série de 7 jours", "Être actif sept jours consécutifs"),
            new BadgeDefinition("LEVEL_5", "Niveau 5", "Atteindre le niveau 5")
    );

    private BadgeCatalog() {
    }

    public static List<BadgeDefinition> all() {
        return DEFINITIONS;
    }

    public static BadgeDefinition required(String code) {
        return DEFINITIONS.stream()
                .filter(definition -> code != null && definition.code().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Badge not found: " + code));
    }
}
