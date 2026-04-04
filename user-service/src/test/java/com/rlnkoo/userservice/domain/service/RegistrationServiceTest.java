package com.rlnkoo.userservice.domain.service;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.userservice.domain.exception.EmailAlreadyUsedException;
import com.rlnkoo.userservice.domain.model.Role;
import com.rlnkoo.userservice.events.producer.UserEventsPublisher;
import com.rlnkoo.userservice.events.types.UserRegisteredPayload;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivationTokenRepository activationTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserEventsPublisher userEventsPublisher;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void shouldThrowEmailAlreadyUsedExceptionWhenEmailExists() {
        // given
        String email = "test@example.com";
        String rawPassword = "Password123!";

        when(userRepository.existsByEmailIgnoreCase(email)).thenReturn(true);

        // when + then
        EmailAlreadyUsedException exception = assertThrows(
                EmailAlreadyUsedException.class,
                () -> registrationService.register(email, rawPassword)
        );

        assertEquals("Email already in use: " + email, exception.getMessage());

        verify(userRepository).existsByEmailIgnoreCase(email);
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(activationTokenRepository, never()).save(any(ActivationTokenEntity.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(tokenService, never()).generateUrlSafeToken(anyInt());
        verify(tokenService, never()).sha256Hex(anyString());
        verify(userEventsPublisher, never()).publish(any(), any());
    }

    @Test
    void shouldRegisterUserCreateActivationTokenAndPublishEvent() {
        // given
        String email = "test@example.com";
        String rawPassword = "Password123!";
        String encodedPassword = "encoded-password";
        String plainToken = "plain-activation-token";
        String tokenHash = "hashed-activation-token";
        UUID userId = UUID.randomUUID();

        when(userRepository.existsByEmailIgnoreCase(email)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(tokenService.generateUrlSafeToken(32)).thenReturn(plainToken);
        when(tokenService.sha256Hex(plainToken)).thenReturn(tokenHash);

        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });

        // when
        registrationService.register(email, rawPassword);

        // then
        verify(userRepository).existsByEmailIgnoreCase(email);
        verify(passwordEncoder).encode(rawPassword);
        verify(tokenService).generateUrlSafeToken(32);
        verify(tokenService).sha256Hex(plainToken);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        UserEntity savedUser = userCaptor.getValue();
        assertEquals(email, savedUser.getEmail());
        assertEquals(encodedPassword, savedUser.getPasswordHash());
        assertFalse(savedUser.isEnabled());
        assertEquals(Set.of(Role.USER), savedUser.getRoles());

        ArgumentCaptor<ActivationTokenEntity> tokenCaptor = ArgumentCaptor.forClass(ActivationTokenEntity.class);
        verify(activationTokenRepository).save(tokenCaptor.capture());

        ActivationTokenEntity savedToken = tokenCaptor.getValue();
        assertEquals(userId, savedToken.getUserId());
        assertEquals(tokenHash, savedToken.getTokenHash());
        assertNotNull(savedToken.getExpiresAt());
        assertTrue(savedToken.getExpiresAt().isAfter(Instant.now().plus(23, ChronoUnit.HOURS)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventEnvelope<UserRegisteredPayload>> eventCaptor =
                ArgumentCaptor.forClass((Class) EventEnvelope.class);

        verify(userEventsPublisher).publish(eq(userId), eventCaptor.capture());

        EventEnvelope<UserRegisteredPayload> event = eventCaptor.getValue();
        assertEquals("UserRegisteredV1", event.eventType());
        assertNotNull(event.eventId());
        assertNotNull(event.occurredAt());

        UserRegisteredPayload payload = event.payload();
        assertEquals(userId, payload.userId());
        assertEquals(email, payload.email());
        assertEquals(plainToken, payload.activationToken());
    }
}