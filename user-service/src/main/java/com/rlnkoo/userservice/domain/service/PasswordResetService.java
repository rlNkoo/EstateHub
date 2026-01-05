package com.rlnkoo.userservice.domain.service;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.userservice.domain.exception.*;
import com.rlnkoo.userservice.events.producer.UserEventsPublisher;
import com.rlnkoo.userservice.events.types.*;
import com.rlnkoo.userservice.persistence.entity.PasswordResetTokenEntity;
import com.rlnkoo.userservice.persistence.entity.UserEntity;
import com.rlnkoo.userservice.persistence.repository.PasswordResetTokenRepository;
import com.rlnkoo.userservice.persistence.repository.UserRepository;
import com.rlnkoo.userservice.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long RESET_TOKEN_TTL_HOURS = 1;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserEventsPublisher userEventsPublisher;

    @Transactional
    public void requestReset(String email) {

        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {

            String plainToken = tokenService.generateUrlSafeToken(32);
            String tokenHash = tokenService.sha256Hex(plainToken);

            PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                    .userId(user.getId())
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().plus(RESET_TOKEN_TTL_HOURS, ChronoUnit.HOURS))
                    .build();

            tokenRepository.save(token);

            PasswordResetRequestedPayload payload =
                    PasswordResetRequestedPayload.builder()
                            .userId(user.getId())
                            .email(user.getEmail())
                            .resetToken(plainToken)
                            .build();

            userEventsPublisher.publish(
                    user.getId(),
                    EventEnvelope.of("PasswordResetRequestedV1", payload)
            );
        });
    }

    @Transactional
    public void confirmReset(String plainToken, String newPassword) {

        String tokenHash = tokenService.sha256Hex(plainToken);

        PasswordResetTokenEntity token = tokenRepository
                .findByTokenHashAndUsedAtIsNull(tokenHash)
                .orElseThrow(InvalidPasswordResetTokenException::new);

        if (token.isUsed()) {
            throw new InvalidPasswordResetTokenException();
        }

        if (token.isExpired(Instant.now())) {
            throw new PasswordResetTokenExpiredException();
        }

        UserEntity user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UserNotFoundException(token.getUserId()));

        user.changePassword(passwordEncoder.encode(newPassword));
        token.markUsed();

        userRepository.save(user);
        tokenRepository.save(token);

        userEventsPublisher.publish(
                user.getId(),
                EventEnvelope.of(
                        "PasswordResetCompletedV1",
                        PasswordResetCompletedPayload.builder()
                                .userId(user.getId())
                                .email(user.getEmail())
                                .completedAt(Instant.now())
                                .build()
                )
        );
    }
}