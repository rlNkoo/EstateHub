package com.rlnkoo.searchservice.security;

import com.rlnkoo.searchservice.domain.exception.AuthenticationRequiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentUserProvider {

    public Optional<Jwt> getCurrentJwtOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            return Optional.empty();
        }

        return Optional.of(jwt);
    }

    public Jwt requireCurrentJwt() {
        return getCurrentJwtOptional()
                .orElseThrow(AuthenticationRequiredException::new);
    }
}