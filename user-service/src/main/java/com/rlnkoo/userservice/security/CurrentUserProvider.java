package com.rlnkoo.userservice.security;

import com.rlnkoo.userservice.domain.exception.AuthenticationRequiredException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class CurrentUserProvider {

    public CurrentUser getCurrentUser() {
        Object principal = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        if (!(principal instanceof Jwt jwt)) {
            throw new AuthenticationRequiredException();
        }

        UUID userId = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        Set<String> roles = Set.copyOf(jwt.getClaimAsStringList("roles"));

        return CurrentUser.builder()
                .userId(userId)
                .email(email)
                .roles(roles)
                .build();
    }
}