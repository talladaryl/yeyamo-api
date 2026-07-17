package com.yeyamo_mobile.api.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;
import com.yeyamo_mobile.api.auth_service.models.RefreshToken;
import com.yeyamo_mobile.api.auth_service.models.User;
import com.yeyamo_mobile.api.auth_service.repository.RefreshTokenRepository;

class RefreshTokenServiceSecurityTests {

    @Test
    void rotatesTokenAndRevokesPreviousOne() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshTokenService service = new RefreshTokenService(repository, 3_600_000);
        User user = new User();
        RefreshToken previous = token(user);
        when(repository.findLockedByTokenHash(any())).thenReturn(Optional.of(previous));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.Rotation rotation = service.rotate("old-refresh-token");

        assertEquals(user, rotation.user());
        assertNotEquals("old-refresh-token", rotation.rawToken());
        verify(repository).save(previous);
    }

    @Test
    void reuseRevokesEveryTokenInTheFamily() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshTokenService service = new RefreshTokenService(repository, 3_600_000);
        User user = new User();
        RefreshToken reused = token(user);
        reused.setRevokedAt(LocalDateTime.now().minusSeconds(1));
        when(repository.findLockedByTokenHash(any())).thenReturn(Optional.of(reused));

        ApiException failure = assertThrows(ApiException.class, () -> service.rotate("stolen-refresh-token"));

        assertEquals("REFRESH_TOKEN_REUSE_DETECTED", failure.getCode());
        verify(repository).deleteByUser(user);
    }

    private RefreshToken token(User user) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash("stored-hash");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        return token;
    }
}
