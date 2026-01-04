package com.rlnkoo.userservice.domain.service;

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

    @Transactional
    public void confirmRegistration(String plainToken) {

        String tokenHash = tokenService.sha256Hex(plainToken);

        ActivationTokenEntity token = activationTokenRepository
                .findByTokenHashAndUsedAtIsNull(tokenHash)
                .orElseThrow(() -> new IllegalStateException("Invalid or already used activation token"));

        if (token.isExpired(Instant.now())) {
            throw new IllegalStateException("Activation token has expired");
        }

        UserEntity user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (user.isEnabled()) {
            return;
        }

        user.activate();
        token.markUsed();

        userRepository.save(user);
        activationTokenRepository.save(token);
    }
}