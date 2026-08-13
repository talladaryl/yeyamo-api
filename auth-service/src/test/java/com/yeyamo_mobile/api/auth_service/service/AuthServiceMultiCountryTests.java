package com.yeyamo_mobile.api.auth_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.yeyamo_mobile.api.auth_service.dto.AuthResponse;
import com.yeyamo_mobile.api.auth_service.dto.RegisterRequest;
import com.yeyamo_mobile.api.auth_service.event.AuthEventOutbox;
import com.yeyamo_mobile.api.auth_service.exception.ApiException;
import com.yeyamo_mobile.api.auth_service.models.Role;
import com.yeyamo_mobile.api.auth_service.models.User;
import com.yeyamo_mobile.api.auth_service.repository.OAuthAccountRepository;
import com.yeyamo_mobile.api.auth_service.repository.RoleRepository;
import com.yeyamo_mobile.api.auth_service.repository.UserRepository;
import com.yeyamo_mobile.api.auth_service.security.AntiBotVerifier;
import com.yeyamo_mobile.api.auth_service.security.JwtService;
import com.yeyamo_mobile.api.auth_service.service.CountryValidationService.CountryValidationResult;

class AuthServiceMultiCountryTests {

    private AuthService authService;
    private UserRepository userRepository;
    private CountryValidationService countryValidationService;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private RefreshTokenService refreshTokenService;
    private AuthEventOutbox eventOutbox;
    private AntiBotVerifier antiBotVerifier;
    private RoleRepository roleRepository;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        countryValidationService = mock(CountryValidationService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        refreshTokenService = mock(RefreshTokenService.class);
        eventOutbox = mock(AuthEventOutbox.class);
        antiBotVerifier = mock(AntiBotVerifier.class);
        roleRepository = mock(RoleRepository.class);

        authService = new AuthService(
                userRepository,
                mock(OAuthAccountRepository.class),
                roleRepository,
                passwordEncoder,
                null,
                jwtService,
                refreshTokenService,
                null,
                null,
                null,
                null,
                eventOutbox,
                antiBotVerifier,
                countryValidationService
        );

        when(roleRepository.findByCode(any())).thenReturn(java.util.Optional.of(new Role()));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.accessTokenExpirationMs()).thenReturn(3600000L);
        when(refreshTokenService.create(any())).thenReturn("refresh-token");
    }

    @Test
    void register_withValidCountry_succeeds() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com",
                "+237699123456",
                "SecurePassword123!",
                "John Doe",
                "turnstile-token",
                "CM",
                null,
                "fr",
                "Africa/Douala"
        );

        CountryValidationResult countryResult = new CountryValidationResult(
                "CM", "LIVE", true, "fr", "Africa/Douala", "XAF"
        );

        when(countryValidationService.validateRegistrationEligibility("CM", false))
                .thenReturn(countryResult);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        verify(countryValidationService).validateRegistrationEligibility("CM", false);
        verify(userRepository).save(argThat(user -> 
                "CM".equals(user.getCountryCode()) &&
                "fr".equals(user.getPreferredLanguageCode()) &&
                "Africa/Douala".equals(user.getTimezone())
        ));
        verify(eventOutbox).userCreated(any(User.class), isNull());
    }

    @Test
    void register_withCityId_validatesCity() {
        UUID cityId = UUID.randomUUID();
        RegisterRequest request = new RegisterRequest(
                "user@example.com",
                "+237699123456",
                "SecurePassword123!",
                "John Doe",
                "turnstile-token",
                "CM",
                cityId,
                "fr",
                "Africa/Douala"
        );

        CountryValidationResult countryResult = new CountryValidationResult(
                "CM", "LIVE", true, "fr", "Africa/Douala", "XAF"
        );

        when(countryValidationService.validateRegistrationEligibility("CM", false))
                .thenReturn(countryResult);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        authService.register(request);

        verify(countryValidationService).validateCity("CM", cityId);
        verify(userRepository).save(argThat(user -> cityId.equals(user.getCityId())));
    }

    @Test
    void register_withDisabledCountry_throwsException() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com",
                "+237699123456",
                "SecurePassword123!",
                "John Doe",
                "turnstile-token",
                "XX",
                null,
                "en",
                "UTC"
        );

        when(countryValidationService.validateRegistrationEligibility("XX", false))
                .thenThrow(new ApiException("COUNTRY_DISABLED", "Ce pays n'est pas disponible", 
                        org.springframework.http.HttpStatus.FORBIDDEN));

        ApiException exception = assertThrows(ApiException.class, 
                () -> authService.register(request));
        assertEquals("COUNTRY_DISABLED", exception.getCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withoutCountryCode_throwsException() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com",
                "+237699123456",
                "SecurePassword123!",
                "John Doe",
                "turnstile-token",
                null,
                null,
                null,
                null
        );

        ApiException exception = assertThrows(ApiException.class, 
                () -> authService.register(request));
        assertEquals("COUNTRY_REQUIRED", exception.getCode());
        verify(countryValidationService, never()).validateRegistrationEligibility(any(), anyBoolean());
    }

    @Test
    void register_usesDefaultLanguageFromCountry_whenNotProvided() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com",
                "+237699123456",
                "SecurePassword123!",
                "John Doe",
                "turnstile-token",
                "CM",
                null,
                null,
                null
        );

        CountryValidationResult countryResult = new CountryValidationResult(
                "CM", "LIVE", true, "fr", "Africa/Douala", "XAF"
        );

        when(countryValidationService.validateRegistrationEligibility("CM", false))
                .thenReturn(countryResult);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        authService.register(request);

        verify(userRepository).save(argThat(user -> 
                "fr".equals(user.getPreferredLanguageCode()) &&
                "Africa/Douala".equals(user.getTimezone())
        ));
    }

    @Test
    void register_withE164PhoneFormat_succeeds() {
        RegisterRequest request = new RegisterRequest(
                null,
                "+237699123456",
                "SecurePassword123!",
                "John Doe",
                "turnstile-token",
                "CM",
                null,
                "fr",
                "Africa/Douala"
        );

        CountryValidationResult countryResult = new CountryValidationResult(
                "CM", "LIVE", true, "fr", "Africa/Douala", "XAF"
        );

        when(countryValidationService.validateRegistrationEligibility("CM", false))
                .thenReturn(countryResult);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        verify(userRepository).save(argThat(user -> 
                "+237699123456".equals(user.getPhone())
        ));
    }
}
