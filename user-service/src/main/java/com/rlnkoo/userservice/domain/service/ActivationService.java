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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ActivationService {

    private final ActivationTokenRepository activationTokenRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final UserEventsPublisher userEventsPublisher;

    @Transactional
    public void confirmRegistration(String plainToken) {

        String tokenHash = tokenService.sha256Hex(plainToken);

        ActivationTokenEntity token = activationTokenRepository
                .findByTokenHashAndUsedAtIsNull(tokenHash)
                .orElseThrow(InvalidActivationTokenException::new);

        if (token.isExpired(Instant.now())) {
            throw new ActivationTokenExpiredException();
        }

        UserEntity user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UserNotFoundException(token.getUserId()));

        if (user.isEnabled()) {
            return;
        }

        user.activate();
        token.markUsed();

        userRepository.save(user);
        activationTokenRepository.save(token);

        UserActivatedPayload payload = UserActivatedPayload.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .activatedAt(user.getConfirmedAt())
                .build();

        EventEnvelope<UserActivatedPayload> event =
                EventEnvelope.of("UserActivatedV1", payload);

        userEventsPublisher.publish(user.getId(), event);
    }
}