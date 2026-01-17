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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final long ACTIVATION_TOKEN_TTL_HOURS = 24;

    private final UserRepository userRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserEventsPublisher userEventsPublisher;

    @Transactional
    public void register(String email, String rawPassword) {
        log.info("Registration attempt email=[{}]", email);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Registration failed: email already used email=[{}]", email);
            throw new EmailAlreadyUsedException(email);
        }

        UserEntity user = UserEntity.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .enabled(false)
                .build();

        user.addRole(Role.USER);
        user = userRepository.save(user);

        log.info("User registered userId=[{}] email=[{}]", user.getId(), user.getEmail());

        String plainToken = tokenService.generateUrlSafeToken(32);
        String tokenHash = tokenService.sha256Hex(plainToken);

        ActivationTokenEntity tokenEntity = ActivationTokenEntity.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(ACTIVATION_TOKEN_TTL_HOURS, ChronoUnit.HOURS))
                .build();

        activationTokenRepository.save(tokenEntity);

        UserRegisteredPayload payload = UserRegisteredPayload.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .activationToken(plainToken)
                .build();

        EventEnvelope<UserRegisteredPayload> event = EventEnvelope.of("UserRegisteredV1", payload);

        userEventsPublisher.publish(user.getId(), event);
        log.debug("Published event UserRegisteredV1 userId=[{}] eventId=[{}]", user.getId(), event.eventId());
    }
}