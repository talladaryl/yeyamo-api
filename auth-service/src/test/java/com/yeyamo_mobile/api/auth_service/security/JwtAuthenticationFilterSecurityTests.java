package com.yeyamo_mobile.api.auth_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.yeyamo_mobile.api.auth_service.enums.UserStatus;
import com.yeyamo_mobile.api.auth_service.models.User;

import jakarta.servlet.FilterChain;

class JwtAuthenticationFilterSecurityTests {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void activeVerifiedEmailAccountIsAuthenticated() throws Exception {
        User user = user("user@example.com", UserStatus.ACTIVE, Instant.now());
        JwtService jwtService = mock(JwtService.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        FilterChain chain = mock(FilterChain.class);
        when(jwtService.extractUserId("token")).thenReturn(42L);
        when(jwtService.isTokenValid("token")).thenReturn(true);
        when(userDetailsService.loadUserById(42L)).thenReturn(new UserPrincipal(user));

        filter(jwtService, userDetailsService).doFilterInternal(request(), response(), chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void suspendedAccountIsNotAuthenticated() throws Exception {
        User user = user("user@example.com", UserStatus.SUSPENDED, Instant.now());
        JwtService jwtService = mock(JwtService.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        FilterChain chain = mock(FilterChain.class);
        when(jwtService.extractUserId("token")).thenReturn(42L);
        when(jwtService.isTokenValid("token")).thenReturn(true);
        when(userDetailsService.loadUserById(42L)).thenReturn(new UserPrincipal(user));
        MockHttpServletResponse response = response();

        filter(jwtService, userDetailsService).doFilterInternal(request(), response, chain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void activeUnverifiedEmailAccountIsNotAuthenticated() throws Exception {
        User user = user("user@example.com", UserStatus.ACTIVE, null);
        JwtService jwtService = mock(JwtService.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        FilterChain chain = mock(FilterChain.class);
        when(jwtService.extractUserId("token")).thenReturn(42L);
        when(jwtService.isTokenValid("token")).thenReturn(true);
        when(userDetailsService.loadUserById(42L)).thenReturn(new UserPrincipal(user));
        MockHttpServletResponse response = response();

        filter(jwtService, userDetailsService).doFilterInternal(request(), response, chain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void phoneOnlyHistoricalAccountRemainsAuthenticated() throws Exception {
        User user = user(null, UserStatus.ACTIVE, null);
        JwtService jwtService = mock(JwtService.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        FilterChain chain = mock(FilterChain.class);
        when(jwtService.extractUserId("token")).thenReturn(42L);
        when(jwtService.isTokenValid("token")).thenReturn(true);
        when(userDetailsService.loadUserById(anyLong())).thenReturn(new UserPrincipal(user));

        filter(jwtService, userDetailsService).doFilterInternal(request(), response(), chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private JwtAuthenticationFilter filter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        return request;
    }

    private MockHttpServletResponse response() {
        return new MockHttpServletResponse();
    }

    private User user(String email, UserStatus status, Instant verifiedAt) {
        User user = new User();
        user.setId(42L);
        user.setEmail(email);
        user.setStatus(status);
        user.setEmailVerifiedAt(verifiedAt);
        return user;
    }
}
