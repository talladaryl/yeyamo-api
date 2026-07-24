package com.yeyamo_mobile.api.user_service.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.user_service.application.port.OutboxPort;
import com.yeyamo_mobile.api.user_service.domain.model.ProfileVisibility;
import com.yeyamo_mobile.api.user_service.domain.model.UserProfile;
import com.yeyamo_mobile.api.user_service.domain.port.UserProfileRepository;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.SpringDataBlockRepository;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.SpringDataFollowRepository;

class SocialSettingsTest {

    @Test
    void partiallyUpdatesAndPersistsSocialSettings() {
        UserProfileRepository profiles = mock(UserProfileRepository.class);
        UserProfile profile = UserProfile.create("42", "Alice");
        when(profiles.findByAuthUserId("42")).thenReturn(Optional.of(profile));
        when(profiles.save(profile)).thenReturn(profile);
        SocialGraphService service = new SocialGraphService(profiles,
                mock(SpringDataFollowRepository.class), mock(SpringDataBlockRepository.class),
                mock(OutboxPort.class));

        var update = new SocialGraphService.SocialSettingsUpdate(
                ProfileVisibility.PRIVATE, false, null, null,
                null, null, null, null, false, false);

        UserProfile result = service.updateSocialSettings("42", update, "correlation");

        assertFalse(result.isShowActivity());
        assertFalse(result.isAllowSuggestions());
        assertFalse(result.isAllowMessagesFromStrangers());
        assertTrue(result.isShowFollowers());
        verify(profiles).save(profile);
    }
}
