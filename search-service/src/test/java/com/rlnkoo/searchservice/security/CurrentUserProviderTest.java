package com.rlnkoo.searchservice.security;

import com.rlnkoo.searchservice.domain.exception.AuthenticationRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CurrentUserProviderTest {

    private final CurrentUserProvider currentUserProvider = new CurrentUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentJwtOptionalWhenPrincipalIsJwt() {
        // given
        UUID userId = UUID.randomUUID();

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .claim("email", "test@example.com")
                .claim("roles", java.util.List.of("USER", "ADMIN"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        var authentication = new UsernamePasswordAuthenticationToken(jwt, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Optional<Jwt> result = currentUserProvider.getCurrentJwtOptional();

        // then
        assertTrue(result.isPresent());
        assertSame(jwt, result.get());
        assertEquals("test-token", result.get().getTokenValue());
        assertEquals(userId.toString(), result.get().getSubject());
    }

    @Test
    void shouldReturnEmptyOptionalWhenAuthenticationIsNull() {
        // given
        SecurityContextHolder.clearContext();

        // when
        Optional<Jwt> result = currentUserProvider.getCurrentJwtOptional();

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyOptionalWhenPrincipalIsNotJwt() {
        // given
        var authentication = new UsernamePasswordAuthenticationToken("not-a-jwt", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Optional<Jwt> result = currentUserProvider.getCurrentJwtOptional();

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnCurrentJwtWhenJwtIsPresent() {
        // given
        Jwt jwt = Jwt.withTokenValue("required-token")
                .header("alg", "HS256")
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        var authentication = new UsernamePasswordAuthenticationToken(jwt, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Jwt result = currentUserProvider.requireCurrentJwt();

        // then
        assertNotNull(result);
        assertSame(jwt, result);
        assertEquals("required-token", result.getTokenValue());
    }

    @Test
    void shouldThrowAuthenticationRequiredExceptionWhenJwtIsMissing() {
        // given
        var authentication = new UsernamePasswordAuthenticationToken("not-a-jwt", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when + then
        AuthenticationRequiredException exception = assertThrows(
                AuthenticationRequiredException.class,
                () -> currentUserProvider.requireCurrentJwt()
        );

        assertEquals("Authentication is required", exception.getMessage());
    }
}