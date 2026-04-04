package com.rlnkoo.userservice.domain.service;

import com.rlnkoo.userservice.api.auth.dto.AuthResponse;
import com.rlnkoo.userservice.config.JwtProperties;
import com.rlnkoo.userservice.domain.exception.InvalidCredentialsException;
import com.rlnkoo.userservice.domain.exception.UserNotActivatedException;
import com.rlnkoo.userservice.domain.model.Role;
import com.rlnkoo.userservice.persistence.entity.UserEntity;
import com.rlnkoo.userservice.persistence.repository.UserRepository;
import com.rlnkoo.userservice.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldThrowInvalidCredentialsExceptionWhenUserNotFound() {
        // given
        String email = "test@example.com";
        String rawPassword = "Password123!";

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(java.util.Optional.empty());

        // when + then
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(email, rawPassword)
        );

        assertEquals("Invalid credentials", exception.getMessage());

        verify(userRepository).findByEmailIgnoreCase(email);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateAccessToken(any(), anyString(), anySet());
        verify(jwtProperties, never()).getExpiresMinutes();
    }

    @Test
    void shouldThrowUserNotActivatedExceptionWhenUserIsNotEnabled() {
        // given
        String email = "test@example.com";
        String rawPassword = "Password123!";
        UUID userId = UUID.randomUUID();

        UserEntity user = UserEntity.builder()
                .id(userId)
                .email(email)
                .passwordHash("encoded-password")
                .enabled(false)
                .roles(Set.of(Role.USER))
                .build();

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(java.util.Optional.of(user));

        // when + then
        UserNotActivatedException exception = assertThrows(
                UserNotActivatedException.class,
                () -> authService.login(email, rawPassword)
        );

        assertEquals("User account is not activated", exception.getMessage());

        verify(userRepository).findByEmailIgnoreCase(email);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateAccessToken(any(), anyString(), anySet());
        verify(jwtProperties, never()).getExpiresMinutes();
    }

    @Test
    void shouldThrowInvalidCredentialsExceptionWhenPasswordDoesNotMatch() {
        // given
        String email = "test@example.com";
        String rawPassword = "Password123!";
        UUID userId = UUID.randomUUID();

        UserEntity user = UserEntity.builder()
                .id(userId)
                .email(email)
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of(Role.USER))
                .build();

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches(rawPassword, "encoded-password")).thenReturn(false);

        // when + then
        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(email, rawPassword)
        );

        assertEquals("Invalid credentials", exception.getMessage());

        verify(userRepository).findByEmailIgnoreCase(email);
        verify(passwordEncoder).matches(rawPassword, "encoded-password");
        verify(jwtService, never()).generateAccessToken(any(), anyString(), anySet());
        verify(jwtProperties, never()).getExpiresMinutes();
    }

    @Test
    void shouldReturnAuthResponseWhenCredentialsAreValid() {
        // given
        String email = "test@example.com";
        String rawPassword = "Password123!";
        String accessToken = "jwt-access-token";
        UUID userId = UUID.randomUUID();

        UserEntity user = UserEntity.builder()
                .id(userId)
                .email(email)
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of(Role.USER, Role.ADMIN))
                .build();

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches(rawPassword, "encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken(userId, email, user.getRoles())).thenReturn(accessToken);
        when(jwtProperties.getExpiresMinutes()).thenReturn(60L);

        // when
        AuthResponse response = authService.login(email, rawPassword);

        // then
        assertNotNull(response);
        assertEquals(accessToken, response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresInSeconds());

        verify(userRepository).findByEmailIgnoreCase(email);
        verify(passwordEncoder).matches(rawPassword, "encoded-password");
        verify(jwtService).generateAccessToken(userId, email, user.getRoles());
        verify(jwtProperties).getExpiresMinutes();
    }
}