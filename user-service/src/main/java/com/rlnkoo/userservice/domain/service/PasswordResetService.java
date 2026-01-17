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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
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
        log.info("Password reset requested email=[{}]", email);

        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {

            String plainToken = tokenService.generateUrlSafeToken(32);
            String tokenHash = tokenService.sha256Hex(plainToken);

            PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                    .userId(user.getId())
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().plus(RESET_TOKEN_TTL_HOURS, ChronoUnit.HOURS))
                    .build();

            tokenRepository.save(token);
            log.info("Password reset token created userId=[{}] email=[{}]", user.getId(), user.getEmail());

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
            log.debug("Published event PasswordResetRequestedV1 userId=[{}]", user.getId());
        });
    }

    @Transactional
    public void confirmReset(String plainToken, String newPassword) {
        log.info("Password reset confirm attempt");
        String tokenHash = tokenService.sha256Hex(plainToken);

        PasswordResetTokenEntity token = tokenRepository
                .findByTokenHashAndUsedAtIsNull(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Password reset confirm failed: invalid token");
                    return new InvalidPasswordResetTokenException();
                });

        if (token.isUsed()) {
            throw new InvalidPasswordResetTokenException();
        }

        if (token.isExpired(Instant.now())) {
            log.warn("Password reset confirm failed: token expired userId=[{}]", token.getUserId());
            throw new PasswordResetTokenExpiredException();
        }

        UserEntity user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> {
                    log.warn("Password reset confirm failed: user not found userId=[{}]", token.getUserId());
                    return new UserNotFoundException(token.getUserId());
                });

        user.changePassword(passwordEncoder.encode(newPassword));
        token.markUsed();

        userRepository.save(user);
        tokenRepository.save(token);

        EventEnvelope<PasswordResetCompletedPayload> event =
                EventEnvelope.of(
                        "PasswordResetCompletedV1",
                        PasswordResetCompletedPayload.builder()
                                .userId(user.getId())
                                .email(user.getEmail())
                                .completedAt(Instant.now())
                                .build()
                );

        userEventsPublisher.publish(user.getId(), event);

        log.info("Password reset completed userId=[{}] email=[{}]", user.getId(), user.getEmail());
        log.debug("Published event PasswordResetCompletedV1 userId=[{}] eventId=[{}]", user.getId(), event.eventId());
    }
}