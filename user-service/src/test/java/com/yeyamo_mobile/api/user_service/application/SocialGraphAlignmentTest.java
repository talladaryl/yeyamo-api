package com.yeyamo_mobile.api.user_service.application;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.user_service.application.port.OutboxPort;
import com.yeyamo_mobile.api.user_service.application.exception.UserProfileException;
import com.yeyamo_mobile.api.user_service.domain.model.UserProfile;
import com.yeyamo_mobile.api.user_service.domain.port.UserProfileRepository;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.FollowEntity;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.SpringDataBlockRepository;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.SpringDataFollowRepository;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.SpringDataMuteRepository;

class SocialGraphAlignmentTest {
    private UserProfileRepository profiles;
    private SpringDataFollowRepository follows;
    private SpringDataBlockRepository blocks;
    private SocialGraphService service;

    @BeforeEach
    void setUp() {
        profiles = mock(UserProfileRepository.class);
        follows = mock(SpringDataFollowRepository.class);
        blocks = mock(SpringDataBlockRepository.class);
        service = new SocialGraphService(profiles, follows, blocks, mock(SpringDataMuteRepository.class), mock(OutboxPort.class));
    }

    @Test
    void followsAProfileUsingThePublicProfileUuid() {
        UserProfile caller = UserProfile.create("42", "Caller");
        UserProfile target = UserProfile.create("99", "Target");
        when(profiles.findByAuthUserId("42")).thenReturn(Optional.of(caller));
        when(profiles.findById(target.getId())).thenReturn(Optional.of(target));
        when(blocks.existsBlockInEitherDirection(caller.getId(), target.getId())).thenReturn(false);
        when(follows.existsByIdFollowerIdAndIdFolloweeId(caller.getId(), target.getId())).thenReturn(false);

        service.follow("42", target.getId(), "correlation");

        verify(follows).save(any(FollowEntity.class));
    }

    @Test
    void removesAFollowerUsingThePublicProfileUuid() {
        UserProfile owner = UserProfile.create("42", "Owner");
        UserProfile follower = UserProfile.create("99", "Follower");
        FollowEntity.FollowId relation = new FollowEntity.FollowId(follower.getId(), owner.getId());
        when(profiles.findByAuthUserId("42")).thenReturn(Optional.of(owner));
        when(profiles.findById(follower.getId())).thenReturn(Optional.of(follower));
        when(follows.existsById(relation)).thenReturn(true);

        service.removeFollower("42", follower.getId(), "correlation");

        verify(follows).deleteById(relation);
    }

    @Test
    void resolvesContentAuthorsToTheirPublicProfileIdentityAndFollowState() {
        UserProfile viewer = UserProfile.create("42", "Viewer");
        UserProfile author = UserProfile.create("99", "Auteur réel");
        author.setAvatarUrl("https://cdn.example/avatar.jpg");
        when(profiles.findByAuthUserId("42")).thenReturn(Optional.of(viewer));
        when(profiles.findByAuthUserIdIn(List.of("99"))).thenReturn(List.of(author));
        when(follows.existsByIdFollowerIdAndIdFolloweeId(viewer.getId(), author.getId())).thenReturn(true);

        var identities = service.resolveContentAuthorIdentities("42", List.of("99"));

        assertEquals(1, identities.size());
        assertEquals(author.getId(), identities.getFirst().profileId());
        assertEquals("Auteur réel", identities.getFirst().displayName());
        assertTrue(identities.getFirst().isFollowing());
    }

    @Test
    void resolvesTheViewersOwnPrivateProfileForSafeFeedOwnershipChecks() {
        UserProfile viewer = UserProfile.create("42", "Viewer");
        viewer.update("Viewer", null, null, null, com.yeyamo_mobile.api.user_service.domain.model.ProfileVisibility.PRIVATE);
        when(profiles.findByAuthUserId("42")).thenReturn(Optional.of(viewer));
        when(profiles.findByAuthUserIdIn(List.of("42"))).thenReturn(List.of(viewer));

        var identities = service.resolveContentAuthorIdentities("42", List.of("42"));

        assertEquals(1, identities.size());
        assertEquals(viewer.getId(), identities.getFirst().profileId());
    }

    @Test
    void preventsSelfFollowAtTheDomainBoundary() {
        UserProfile viewer = UserProfile.create("42", "Viewer");
        when(profiles.findByAuthUserId("42")).thenReturn(Optional.of(viewer));
        when(profiles.findById(viewer.getId())).thenReturn(Optional.of(viewer));

        var error = assertThrows(UserProfileException.class,
                () -> service.follow("42", viewer.getId(), "correlation"));

        assertEquals("CANNOT_FOLLOW_YOURSELF", error.getCode());
    }
}
