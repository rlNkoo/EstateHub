package com.rlnkoo.userservice.domain.service;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.userservice.domain.exception.InvalidPasswordResetTokenException;
import com.rlnkoo.userservice.domain.exception.PasswordResetTokenExpiredException;
import com.rlnkoo.userservice.domain.exception.UserNotFoundException;
import com.rlnkoo.userservice.events.producer.UserEventsPublisher;
import com.rlnkoo.userservice.events.types.PasswordResetCompletedPayload;
import com.rlnkoo.userservice.events.types.PasswordResetRequestedPayload;
import com.rlnkoo.userservice.persistence.entity.PasswordResetTokenEntity;
import com.rlnkoo.userservice.persistence.entity.UserEntity;
import com.rlnkoo.userservice.persistence.repository.PasswordResetTokenRepository;
import com.rlnkoo.userservice.persistence.repository.UserRepository;
import com.rlnkoo.userservice.security.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserEventsPublisher userEventsPublisher;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void shouldDoNothingWhenUserWithEmailDoesNotExist() {
        // given
        String email = "missing@example.com";

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(java.util.Optional.empty());

        // when
        passwordResetService.requestReset(email);

        // then
        verify(userRepository).findByEmailIgnoreCase(email);
        verify(tokenService, never()).generateUrlSafeToken(anyInt());
        verify(tokenService, never()).sha256Hex(anyString());
        verify(tokenRepository, never()).save(any());
        verify(userEventsPublisher, never()).publish(any(), any());
    }

    @Test
    void shouldCreateResetTokenAndPublishEventWhenUserExists() {
        // given
        String email = "test@example.com";
        String plainToken = "plain-reset-token";
        String tokenHash = "hashed-reset-token";
        UUID userId = UUID.randomUUID();

        UserEntity user = UserEntity.builder()
                .id(userId)
                .email(email)
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of())
                .build();

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(java.util.Optional.of(user));
        when(tokenService.generateUrlSafeToken(32)).thenReturn(plainToken);
        when(tokenService.sha256Hex(plainToken)).thenReturn(tokenHash);

        // when
        passwordResetService.requestReset(email);

        // then
        verify(userRepository).findByEmailIgnoreCase(email);
        verify(tokenService).generateUrlSafeToken(32);
        verify(tokenService).sha256Hex(plainToken);

        ArgumentCaptor<PasswordResetTokenEntity> tokenCaptor =
                ArgumentCaptor.forClass(PasswordResetTokenEntity.class);

        verify(tokenRepository).save(tokenCaptor.capture());

        PasswordResetTokenEntity savedToken = tokenCaptor.getValue();
        assertEquals(userId, savedToken.getUserId());
        assertEquals(tokenHash, savedToken.getTokenHash());
        assertNotNull(savedToken.getExpiresAt());
        assertTrue(savedToken.getExpiresAt().isAfter(Instant.now().plus(59, ChronoUnit.MINUTES)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventEnvelope<PasswordResetRequestedPayload>> eventCaptor =
                ArgumentCaptor.forClass((Class) EventEnvelope.class);

        verify(userEventsPublisher).publish(eq(userId), eventCaptor.capture());

        EventEnvelope<PasswordResetRequestedPayload> event = eventCaptor.getValue();
        assertEquals("PasswordResetRequestedV1", event.eventType());
        assertNotNull(event.eventId());
        assertNotNull(event.occurredAt());

        PasswordResetRequestedPayload payload = event.payload();
        assertEquals(userId, payload.userId());
        assertEquals(email, payload.email());
        assertEquals(plainToken, payload.resetToken());
    }

    @Test
    void shouldThrowInvalidPasswordResetTokenExceptionWhenTokenNotFound() {
        // given
        String plainToken = "plain-token";
        String tokenHash = "hashed-token";

        when(tokenService.sha256Hex(plainToken)).thenReturn(tokenHash);
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash))
                .thenReturn(java.util.Optional.empty());

        // when + then
        InvalidPasswordResetTokenException exception = assertThrows(
                InvalidPasswordResetTokenException.class,
                () -> passwordResetService.confirmReset(plainToken, "NewPassword123!")
        );

        assertEquals("Invalid or already used password reset token", exception.getMessage());

        verify(tokenService).sha256Hex(plainToken);
        verify(tokenRepository).findByTokenHashAndUsedAtIsNull(tokenHash);
        verify(userRepository, never()).findById(any());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).save(any());
        verify(userEventsPublisher, never()).publish(any(), any());
    }

    @Test
    void shouldThrowPasswordResetTokenExpiredExceptionWhenTokenExpired() {
        // given
        String plainToken = "plain-token";
        String tokenHash = "hashed-token";
        UUID userId = UUID.randomUUID();

        PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().minusSeconds(60))
                .build();

        when(tokenService.sha256Hex(plainToken)).thenReturn(tokenHash);
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash))
                .thenReturn(java.util.Optional.of(token));

        // when + then
        PasswordResetTokenExpiredException exception = assertThrows(
                PasswordResetTokenExpiredException.class,
                () -> passwordResetService.confirmReset(plainToken, "NewPassword123!")
        );

        assertEquals("Password reset token has expired", exception.getMessage());

        verify(tokenService).sha256Hex(plainToken);
        verify(tokenRepository).findByTokenHashAndUsedAtIsNull(tokenHash);
        verify(userRepository, never()).findById(any());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).save(any());
        verify(userEventsPublisher, never()).publish(any(), any());
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserForTokenDoesNotExist() {
        // given
        String plainToken = "plain-token";
        String tokenHash = "hashed-token";
        UUID userId = UUID.randomUUID();

        PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(tokenService.sha256Hex(plainToken)).thenReturn(tokenHash);
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash))
                .thenReturn(java.util.Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.empty());

        // when + then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> passwordResetService.confirmReset(plainToken, "NewPassword123!")
        );

        assertEquals("User not found: " + userId, exception.getMessage());

        verify(tokenService).sha256Hex(plainToken);
        verify(tokenRepository).findByTokenHashAndUsedAtIsNull(tokenHash);
        verify(userRepository).findById(userId);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).save(any());
        verify(userEventsPublisher, never()).publish(any(), any());
    }

    @Test
    void shouldResetPasswordMarkTokenUsedAndPublishEvent() {
        // given
        String plainToken = "plain-token";
        String tokenHash = "hashed-token";
        String newPassword = "NewPassword123!";
        String encodedPassword = "new-encoded-password";
        UUID userId = UUID.randomUUID();

        PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        UserEntity user = UserEntity.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("old-encoded-password")
                .enabled(true)
                .roles(Set.of())
                .build();

        when(tokenService.sha256Hex(plainToken)).thenReturn(tokenHash);
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash))
                .thenReturn(java.util.Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

        // when
        passwordResetService.confirmReset(plainToken, newPassword);

        // then
        assertEquals(encodedPassword, user.getPasswordHash());
        assertNotNull(token.getUsedAt());

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertSame(user, userCaptor.getValue());

        ArgumentCaptor<PasswordResetTokenEntity> tokenCaptor =
                ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertSame(token, tokenCaptor.getValue());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventEnvelope<PasswordResetCompletedPayload>> eventCaptor =
                ArgumentCaptor.forClass((Class) EventEnvelope.class);

        verify(userEventsPublisher).publish(eq(userId), eventCaptor.capture());

        EventEnvelope<PasswordResetCompletedPayload> event = eventCaptor.getValue();
        assertEquals("PasswordResetCompletedV1", event.eventType());
        assertNotNull(event.eventId());
        assertNotNull(event.occurredAt());

        PasswordResetCompletedPayload payload = event.payload();
        assertEquals(userId, payload.userId());
        assertEquals("test@example.com", payload.email());
        assertNotNull(payload.completedAt());

        verify(passwordEncoder).encode(newPassword);
    }
}