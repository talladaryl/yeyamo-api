package com.yeyamo_mobile.api.user_service.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserProfileTests {

    @Test
    void createsSecureDefaults() {
        UserProfile profile = UserProfile.create("42", " Alice ");

        assertThat(profile.getId()).isNotNull();
        assertThat(profile.getDisplayName()).isEqualTo("Alice");
        assertThat(profile.getLanguage()).isEqualTo(Language.FRENCH);
        assertThat(profile.getVisibility()).isEqualTo(ProfileVisibility.PUBLIC);
        assertThat(profile.getStatus()).isEqualTo(ProfileStatus.ACTIVE);
        assertThat(profile.isNotificationsEnabled()).isTrue();
        assertThat(profile.isLocationSharingEnabled()).isFalse();
    }

    @Test
    void privateProfileIsOnlyVisibleToItsOwner() {
        UserProfile profile = UserProfile.create("42", "Alice");
        profile.update("Alice", null, null, Language.FRENCH, ProfileVisibility.PRIVATE);

        assertThat(profile.isVisibleTo("42")).isTrue();
        assertThat(profile.isVisibleTo("99")).isFalse();
        assertThat(profile.isVisibleTo(null)).isFalse();
    }

    @Test
    void deleteAnonymizesPublicDataAndDisablesPreferences() {
        UserProfile profile = UserProfile.create("42", "Alice");
        profile.update("Alice", "https://img.test/alice.jpg", "Bio", Language.ENGLISH, ProfileVisibility.PUBLIC);
        profile.updatePreferences(true, true, 5L);

        profile.delete();

        assertThat(profile.getStatus()).isEqualTo(ProfileStatus.DELETED);
        assertThat(profile.getDisplayName()).isEqualTo("Utilisateur supprimé");
        assertThat(profile.getAvatarUrl()).isNull();
        assertThat(profile.getBio()).isNull();
        assertThat(profile.isNotificationsEnabled()).isFalse();
        assertThat(profile.isLocationSharingEnabled()).isFalse();
        assertThat(profile.getDeletedAt()).isNotNull();
    }
}
