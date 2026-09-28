package com.yeyamo_mobile.api.user_service.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.user_service.application.port.OutboxPort;
import com.yeyamo_mobile.api.user_service.domain.model.UserProfile;
import com.yeyamo_mobile.api.user_service.domain.port.UserProfileRepository;

class UserProfileMultiCountryTests {

    private UserProfileService service;
    private UserProfileRepository repository;
    private OutboxPort outbox;

    @BeforeEach
    void setup() {
        repository = mock(UserProfileRepository.class);
        outbox = mock(OutboxPort.class);
        service = new UserProfileService(repository, outbox);
    }

    @Test
    void createFromIdentityWithLocation_setsGeographicFields() {
        String authUserId = "user-123";
        UUID cityId = UUID.randomUUID();

        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile profile = service.createFromIdentityWithLocation(
                authUserId, "John Doe", "CM", cityId, "fr", "Africa/Douala", null
        );

        assertNotNull(profile);
        assertEquals("CM", profile.getCountryCode());
        assertEquals(cityId, profile.getCityId());
        assertEquals("fr", profile.getPreferredLanguageCode());
        assertEquals("Africa/Douala", profile.getTimezone());
        verify(repository).save(any(UserProfile.class));
        verify(outbox).append(eq("profile.created"), any(), eq(authUserId), isNull(), any());
    }

    @Test
    void initialEvent_completesBlankProfileCreatedBeforeKafkaDelivery() {
        String authUserId = "user-123";
        UUID cityId = UUID.randomUUID();
        UserProfile blankProfile = UserProfile.create(authUserId, "John Doe");
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.of(blankProfile));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile completed = service.createFromIdentityWithLocation(
                authUserId, "John Doe", "CM", cityId, "fr", "Africa/Douala", "corr-1");

        assertEquals("CM", completed.getCountryCode());
        assertEquals(cityId, completed.getCityId());
        assertEquals("fr", completed.getPreferredLanguageCode());
        assertEquals("Africa/Douala", completed.getTimezone());
        verify(repository).save(blankProfile);
        verify(outbox).append(eq("profile.location_updated"), any(), eq(authUserId), eq("corr-1"), any());
    }

    @Test
    void initialEvent_neverOverridesAnExistingCountryChoice() {
        String authUserId = "user-123";
        UserProfile profile = UserProfile.create(authUserId, "John Doe");
        profile.setCountryCode("SN");
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.of(profile));

        UserProfile unchanged = service.createFromIdentityWithLocation(
                authUserId, "John Doe", "CM", null, null, null, null);

        assertEquals("SN", unchanged.getCountryCode());
        verify(repository, never()).save(any(UserProfile.class));
    }

    @Test
    void profileRead_canMaterialiseCountryFromTheSignedAuthContext() {
        String authUserId = "user-123";
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile profile = service.getOrCreateWithInitialLocation(
                authUserId, "John Doe", "CM", null, null, null, "corr-2");

        assertEquals("CM", profile.getCountryCode());
        verify(outbox).append(eq("profile.created"), any(), eq(authUserId), eq("corr-2"), any());
    }

    @Test
    void updateLocation_updatesGeographicFields() {
        String authUserId = "user-123";
        UUID cityId = UUID.randomUUID();
        UUID adminLevel1Id = UUID.randomUUID();

        UserProfile existing = UserProfile.create(authUserId, "John Doe");
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.of(existing));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile updated = service.updateLocation(authUserId, "SN", adminLevel1Id, null, 
                cityId, null, "Africa/Dakar", null);

        assertEquals("SN", updated.getCountryCode());
        assertEquals(adminLevel1Id, updated.getAdminLevel1Id());
        assertEquals(cityId, updated.getCityId());
        assertEquals("Africa/Dakar", updated.getTimezone());
        verify(outbox).append(eq("profile.location_updated"), any(), eq(authUserId), isNull(), any());
    }

    @Test
    void updateLanguagePreferences_updatesLanguageFields() {
        String authUserId = "user-123";
        Set<String> contentLanguages = Set.of("fr", "en", "bam");

        UserProfile existing = UserProfile.create(authUserId, "John Doe");
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.of(existing));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile updated = service.updateLanguagePreferences(authUserId, "fr", contentLanguages, null);

        assertEquals("fr", updated.getPreferredLanguageCode());
        assertEquals(contentLanguages, updated.getContentLanguages());
        verify(outbox).append(eq("profile.language_updated"), any(), eq(authUserId), isNull(), any());
    }

    @Test
    void updateDiscoveryPreferences_updatesDiscoveryFields() {
        String authUserId = "user-123";
        Set<String> contentCountries = Set.of("CM", "SN", "CI");

        UserProfile existing = UserProfile.create(authUserId, "John Doe");
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.of(existing));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile updated = service.updateDiscoveryPreferences(authUserId, contentCountries, 
                50, true, "XAF", null);

        assertEquals(contentCountries, updated.getContentCountries());
        assertEquals(50, updated.getLocalRadiusKm());
        assertTrue(updated.isDiscoverAfricanContent());
        assertEquals("XAF", updated.getPreferredCurrencyCode());
        verify(outbox).append(eq("profile.discovery_preferences_updated"), any(), eq(authUserId), isNull(), any());
    }

    @Test
    void updateLocation_nullCountryCode_keepsExisting() {
        String authUserId = "user-123";
        UUID cityId = UUID.randomUUID();

        UserProfile existing = UserProfile.create(authUserId, "John Doe");
        existing.setCountryCode("CM");
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.of(existing));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile updated = service.updateLocation(authUserId, null, null, null, 
                cityId, null, null, null);

        assertEquals("CM", updated.getCountryCode());
        assertEquals(cityId, updated.getCityId());
    }

    @Test
    void updateLanguagePreferences_emptyContentLanguages_clearsExisting() {
        String authUserId = "user-123";

        UserProfile existing = UserProfile.create(authUserId, "John Doe");
        existing.setContentLanguages(Set.of("fr", "en"));
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.of(existing));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile updated = service.updateLanguagePreferences(authUserId, "fr", Set.of(), null);

        assertTrue(updated.getContentLanguages().isEmpty());
    }

    @Test
    void updateDiscoveryPreferences_africanContentDefault_keepsTrue() {
        String authUserId = "user-123";

        UserProfile existing = UserProfile.create(authUserId, "John Doe");
        existing.setDiscoverAfricanContent(true);
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.of(existing));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile updated = service.updateDiscoveryPreferences(authUserId, Set.of("CM"), 
                100, null, "XAF", null);

        assertTrue(updated.isDiscoverAfricanContent());
    }

    @Test
    void updateDiscoveryPreferences_disableAfricanContent_updates() {
        String authUserId = "user-123";

        UserProfile existing = UserProfile.create(authUserId, "John Doe");
        existing.setDiscoverAfricanContent(true);
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.of(existing));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile updated = service.updateDiscoveryPreferences(authUserId, Set.of("CM"), 
                50, false, "XAF", null);

        assertFalse(updated.isDiscoverAfricanContent());
    }
}
