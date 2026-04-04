package com.rlnkoo.userservice.security;

import com.rlnkoo.userservice.domain.exception.AuthenticationRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CurrentUserProviderTest {

    private final CurrentUserProvider currentUserProvider = new CurrentUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentUserWhenPrincipalIsJwt() {
        // given
        UUID userId = UUID.randomUUID();

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .claim("email", "test@example.com")
                .claim("roles", List.of("USER", "ADMIN"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        var authentication = new UsernamePasswordAuthenticationToken(jwt, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        CurrentUser currentUser = currentUserProvider.getCurrentUser();

        // then
        assertNotNull(currentUser);
        assertEquals(userId, currentUser.userId());
        assertEquals("test@example.com", currentUser.email());
        assertEquals(Set.of("USER", "ADMIN"), currentUser.roles());
    }

    @Test
    void shouldThrowAuthenticationRequiredExceptionWhenPrincipalIsNotJwt() {
        // given
        var authentication = new UsernamePasswordAuthenticationToken("not-a-jwt", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when + then
        AuthenticationRequiredException exception = assertThrows(
                AuthenticationRequiredException.class,
                () -> currentUserProvider.getCurrentUser()
        );

        assertEquals("Authentication is required", exception.getMessage());
    }
}