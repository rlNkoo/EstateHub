package com.rlnkoo.mediaservice.security;

import com.rlnkoo.commonsecurity.Claims;
import com.rlnkoo.mediaservice.domain.exception.AuthenticationRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CurrentUserProviderTest {

    private final CurrentUserProvider currentUserProvider = new CurrentUserProvider();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnEmptyWhenAuthenticationIsNull() {
        // given
        SecurityContextHolder.clearContext();

        // when
        Optional<CurrentUser> result = currentUserProvider.getCurrentUserOptional();

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenPrincipalIsNotJwt() {
        // given
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("plain-principal", null);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Optional<CurrentUser> result = currentUserProvider.getCurrentUserOptional();

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnCurrentUserWhenJwtIsPresent() {
        // given
        UUID userId = UUID.randomUUID();

        Jwt jwt = new Jwt(
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                java.util.Map.of("alg", "HS256"),
                java.util.Map.of(
                        Claims.EMAIL, "user@example.com",
                        Claims.ROLES, List.of("USER", "ADMIN")
                )
        ) {
            @Override
            public String getSubject() {
                return userId.toString();
            }
        };

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(jwt, null);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Optional<CurrentUser> result = currentUserProvider.getCurrentUserOptional();

        // then
        assertTrue(result.isPresent());

        CurrentUser currentUser = result.get();
        assertEquals(userId, currentUser.userId());
        assertEquals("user@example.com", currentUser.email());
        assertEquals(Set.of("USER", "ADMIN"), currentUser.roles());
    }

    @Test
    void shouldReturnEmptyRolesWhenRolesClaimIsMissing() {
        // given
        UUID userId = UUID.randomUUID();

        Jwt jwt = new Jwt(
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                java.util.Map.of("alg", "HS256"),
                java.util.Map.of(
                        Claims.EMAIL, "user@example.com"
                )
        ) {
            @Override
            public String getSubject() {
                return userId.toString();
            }
        };

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(jwt, null);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Optional<CurrentUser> result = currentUserProvider.getCurrentUserOptional();

        // then
        assertTrue(result.isPresent());

        CurrentUser currentUser = result.get();
        assertEquals(userId, currentUser.userId());
        assertEquals("user@example.com", currentUser.email());
        assertEquals(Set.of(), currentUser.roles());
    }

    @Test
    void shouldThrowAuthenticationRequiredExceptionWhenRequireCurrentUserAndNoAuthentication() {
        // given
        SecurityContextHolder.clearContext();

        // when + then
        AuthenticationRequiredException exception = assertThrows(
                AuthenticationRequiredException.class,
                () -> currentUserProvider.requireCurrentUser()
        );

        assertEquals("Authentication is required", exception.getMessage());
    }

    @Test
    void shouldThrowAuthenticationRequiredExceptionWhenRequireCurrentUserAndPrincipalIsNotJwt() {
        // given
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("plain-principal", null);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when + then
        AuthenticationRequiredException exception = assertThrows(
                AuthenticationRequiredException.class,
                () -> currentUserProvider.requireCurrentUser()
        );

        assertEquals("Authentication is required", exception.getMessage());
    }

    @Test
    void shouldReturnCurrentUserWhenRequireCurrentUserAndJwtIsPresent() {
        // given
        UUID userId = UUID.randomUUID();

        Jwt jwt = new Jwt(
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                java.util.Map.of("alg", "HS256"),
                java.util.Map.of(
                        Claims.EMAIL, "admin@example.com",
                        Claims.ROLES, List.of("ADMIN")
                )
        ) {
            @Override
            public String getSubject() {
                return userId.toString();
            }
        };

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(jwt, null);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        CurrentUser result = currentUserProvider.requireCurrentUser();

        // then
        assertEquals(userId, result.userId());
        assertEquals("admin@example.com", result.email());
        assertEquals(Set.of("ADMIN"), result.roles());
    }
}