package com.yeyamo_mobile.api.user_service.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.user_service.application.port.OutboxPort;
import com.yeyamo_mobile.api.user_service.domain.model.UserProfile;
import com.yeyamo_mobile.api.user_service.domain.port.UserProfileRepository;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.FollowEntity;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.SpringDataBlockRepository;
import com.yeyamo_mobile.api.user_service.infrastructure.persistence.SpringDataFollowRepository;

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
        service = new SocialGraphService(profiles, follows, blocks, mock(OutboxPort.class));
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
}
