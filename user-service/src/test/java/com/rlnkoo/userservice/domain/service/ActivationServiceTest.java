package com.rlnkoo.userservice.domain.service;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.userservice.domain.exception.ActivationTokenExpiredException;
import com.rlnkoo.userservice.domain.exception.InvalidActivationTokenException;
import com.rlnkoo.userservice.domain.exception.UserNotFoundException;
import com.rlnkoo.userservice.events.producer.UserEventsPublisher;
import com.rlnkoo.userservice.events.types.UserActivatedPayload;
import com.rlnkoo.userservice.persistence.entity.ActivationTokenEntity;
import com.rlnkoo.userservice.persistence.entity.UserEntity;
import com.rlnkoo.userservice.persistence.repository.ActivationTokenRepository;
import com.rlnkoo.userservice.persistence.repository.UserRepository;
import com.rlnkoo.userservice.security.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivationServiceTest {

    @Mock
    private ActivationTokenRepository activationTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserEventsPublisher userEventsPublisher;

    @InjectMocks
    private ActivationService activationService;

    @Test
    void shouldThrowInvalidActivationTokenExceptionWhenTokenNotFound() {
        // given
        String plainToken = "plain-token";
        String tokenHash = "token-hash";

        when(tokenService.sha256Hex(plainToken)).thenReturn(tokenHash);
        when(activationTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash))
                .thenReturn(java.util.Optional.empty());

        // when + then
        assertThrows(
                InvalidActivationTokenException.class,
                () -> activationService.confirmRegistration(plainToken)
        );

        verify(tokenService).sha256Hex(plainToken);
        verify(activationTokenRepository).findByTokenHashAndUsedAtIsNull(tokenHash);
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
        verify(activationTokenRepository, never()).save(any());
        verify(userEventsPublisher, never()).publish(any(), any());
    }

    @Test
    void shouldThrowActivationTokenExpiredExceptionWhenTokenExpired() {
        // given
        String plainToken = "plain-token";
        String tokenHash = "token-hash";
        UUID userId = UUID.randomUUID();

        ActivationTokenEntity token = ActivationTokenEntity.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().minusSeconds(60))
                .build();

        when(tokenService.sha256Hex(plainToken)).thenReturn(tokenHash);
        when(activationTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash))
                .thenReturn(java.util.Optional.of(token));

        // when + then
        assertThrows(
                ActivationTokenExpiredException.class,
                () -> activationService.confirmRegistration(plainToken)
        );

        verify(tokenService).sha256Hex(plainToken);
        verify(activationTokenRepository).findByTokenHashAndUsedAtIsNull(tokenHash);
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
        verify(activationTokenRepository, never()).save(any());
        verify(userEventsPublisher, never()).publish(any(), any());
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserForTokenDoesNotExist() {
        // given
        String plainToken = "plain-token";
        String tokenHash = "token-hash";
        UUID userId = UUID.randomUUID();

        ActivationTokenEntity token = ActivationTokenEntity.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(tokenService.sha256Hex(plainToken)).thenReturn(tokenHash);
        when(activationTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash))
                .thenReturn(java.util.Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.empty());

        // when + then
        assertThrows(
                UserNotFoundException.class,
                () -> activationService.confirmRegistration(plainToken)
        );

        verify(tokenService).sha256Hex(plainToken);
        verify(activationTokenRepository).findByTokenHashAndUsedAtIsNull(tokenHash);
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
        verify(activationTokenRepository, never()).save(any());
        verify(userEventsPublisher, never()).publish(any(), any());
    }

    @Test
    void shouldReturnWithoutSavingWhenUserAlreadyActivated() {
        // given
        String plainToken = "plain-token";
        String tokenHash = "token-hash";
        UUID userId = UUID.randomUUID();

        ActivationTokenEntity token = ActivationTokenEntity.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        UserEntity user = UserEntity.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("hashed-password")
                .enabled(true)
                .roles(Set.of())
                .build();

        when(tokenService.sha256Hex(plainToken)).thenReturn(tokenHash);
        when(activationTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash))
                .thenReturn(java.util.Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        // when
        activationService.confirmRegistration(plainToken);

        // then
        verify(tokenService).sha256Hex(plainToken);
        verify(activationTokenRepository).findByTokenHashAndUsedAtIsNull(tokenHash);
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
        verify(activationTokenRepository, never()).save(any());
        verify(userEventsPublisher, never()).publish(any(), any());

        assertTrue(user.isEnabled());
        assertNull(token.getUsedAt());
    }

    @Test
    void shouldActivateUserMarkTokenUsedAndPublishEvent() {
        // given
        String plainToken = "plain-token";
        String tokenHash = "token-hash";
        UUID userId = UUID.randomUUID();

        ActivationTokenEntity token = ActivationTokenEntity.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        UserEntity user = UserEntity.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("hashed-password")
                .enabled(false)
                .roles(Set.of())
                .build();

        when(tokenService.sha256Hex(plainToken)).thenReturn(tokenHash);
        when(activationTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash))
                .thenReturn(java.util.Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        // when
        activationService.confirmRegistration(plainToken);

        // then
        assertTrue(user.isEnabled());
        assertNotNull(user.getConfirmedAt());
        assertNotNull(token.getUsedAt());

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertSame(user, userCaptor.getValue());

        ArgumentCaptor<ActivationTokenEntity> tokenCaptor = ArgumentCaptor.forClass(ActivationTokenEntity.class);
        verify(activationTokenRepository).save(tokenCaptor.capture());
        assertSame(token, tokenCaptor.getValue());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventEnvelope<UserActivatedPayload>> eventCaptor =
                ArgumentCaptor.forClass((Class) EventEnvelope.class);

        verify(userEventsPublisher).publish(eq(userId), eventCaptor.capture());

        EventEnvelope<UserActivatedPayload> event = eventCaptor.getValue();
        assertEquals("UserActivatedV1", event.eventType());
        assertNotNull(event.eventId());
        assertNotNull(event.occurredAt());

        UserActivatedPayload payload = event.payload();
        assertEquals(userId, payload.userId());
        assertEquals("test@example.com", payload.email());
        assertNotNull(payload.activatedAt());
        assertEquals(user.getConfirmedAt(), payload.activatedAt());
    }
}