package com.rlnkoo.userservice.domain.service;

import com.rlnkoo.userservice.api.auth.dto.AuthResponse;
import com.rlnkoo.userservice.config.JwtProperties;
import com.rlnkoo.userservice.domain.exception.InvalidCredentialsException;
import com.rlnkoo.userservice.domain.exception.UserNotActivatedException;
import com.rlnkoo.userservice.persistence.entity.UserEntity;
import com.rlnkoo.userservice.persistence.repository.UserRepository;
import com.rlnkoo.userservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Transactional(readOnly = true)
    public AuthResponse login(String email, String rawPassword) {
        log.info("Login attempt email=[{}]", email);

        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Login failed: invalid credentials email=[{}]", email);
                    return new InvalidCredentialsException();
                });

        if (!user.isEnabled()) {
            log.warn("Login blocked: user not activated userId=[{}] email=[{}]", user.getId(), user.getEmail());
            throw new UserNotActivatedException();
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            log.warn("Login failed: bad credentials userId=[{}] email=[{}]", user.getId(), user.getEmail());
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRoles()
        );

        long expiresInSeconds = jwtProperties.getExpiresMinutes() * 60;
        log.info("Login success userId=[{}] roles=[{}]", user.getId(), user.getRoles());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInSeconds(expiresInSeconds)
                .build();
    }
}