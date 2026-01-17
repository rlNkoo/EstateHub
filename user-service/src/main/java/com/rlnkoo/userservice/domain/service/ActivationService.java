package com.rlnkoo.userservice.domain.service;

import com.rlnkoo.commonevents.EventEnvelope;
import com.rlnkoo.userservice.domain.exception.ActivationTokenExpiredException;
import com.rlnkoo.userservice.domain.exception.UserNotFoundException;
import com.rlnkoo.userservice.domain.exception.InvalidActivationTokenException;
import com.rlnkoo.userservice.events.producer.UserEventsPublisher;
import com.rlnkoo.userservice.events.types.UserActivatedPayload;
import com.rlnkoo.userservice.persistence.entity.ActivationTokenEntity;
import com.rlnkoo.userservice.persistence.entity.UserEntity;
import com.rlnkoo.userservice.persistence.repository.ActivationTokenRepository;
import com.rlnkoo.userservice.persistence.repository.UserRepository;
import com.rlnkoo.userservice.security.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivationService {

    private final ActivationTokenRepository activationTokenRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final UserEventsPublisher userEventsPublisher;

    @Transactional
    public void confirmRegistration(String plainToken) {
        log.info("Confirm registration attempt");
        String tokenHash = tokenService.sha256Hex(plainToken);

        ActivationTokenEntity token = activationTokenRepository
                .findByTokenHashAndUsedAtIsNull(tokenHash)
                .orElseThrow(InvalidActivationTokenException::new);

        if (token.isUsed()) {
            log.warn("Confirm registration failed: token already used userId=[{}]", token.getUserId());
            throw new InvalidActivationTokenException();
        }

        if (token.isExpired(Instant.now())) {
            log.warn("Confirm registration failed: token expired userId=[{}]", token.getUserId());
            throw new ActivationTokenExpiredException();
        }

        UserEntity user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> {
                    log.warn("Confirm registration failed: user not found userId=[{}]", token.getUserId());
                    return new UserNotFoundException(token.getUserId());
                });

        if (user.isEnabled()) {
            log.info("User already activated userId=[{}]", user.getId());
            return;
        }

        user.activate();
        token.markUsed();

        userRepository.save(user);
        activationTokenRepository.save(token);

        log.info("User activated userId=[{}] email=[{}]", user.getId(), user.getEmail());

        UserActivatedPayload payload = UserActivatedPayload.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .activatedAt(user.getConfirmedAt())
                .build();

        EventEnvelope<UserActivatedPayload> event =
                EventEnvelope.of("UserActivatedV1", payload);

        userEventsPublisher.publish(user.getId(), event);
        log.debug("Published event UserActivatedV1 userId=[{}] eventId=[{}]", user.getId(), event.eventId());
    }
}