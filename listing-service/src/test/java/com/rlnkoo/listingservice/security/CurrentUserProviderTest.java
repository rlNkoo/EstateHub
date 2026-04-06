package com.rlnkoo.listingservice.security;

import com.rlnkoo.commonsecurity.Claims;
import com.rlnkoo.listingservice.domain.exception.AuthenticationRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
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
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnEmptyOptionalWhenAuthenticationIsNull() {
        // given
        SecurityContextHolder.clearContext();

        // when
        Optional<CurrentUser> result = currentUserProvider.getCurrentUserOptional();

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyOptionalWhenPrincipalIsNotJwt() {
        // given
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("not-jwt", null);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Optional<CurrentUser> result = currentUserProvider.getCurrentUserOptional();

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnCurrentUserWhenJwtIsValid() {
        // given
        UUID userId = UUID.randomUUID();

        Jwt jwt = jwt(userId.toString(), "user@example.com", List.of("USER", "ADMIN"));

        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(jwt, null);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Optional<CurrentUser> result = currentUserProvider.getCurrentUserOptional();

        // then
        assertTrue(result.isPresent());

        CurrentUser user = result.get();
        assertEquals(userId, user.userId());
        assertEquals("user@example.com", user.email());
        assertEquals(Set.of("USER", "ADMIN"), user.roles());
    }

    @Test
    void shouldReturnCurrentUserWithEmptyRolesWhenRolesClaimIsNull() {
        // given
        UUID userId = UUID.randomUUID();

        Jwt jwt = jwtWithoutRoles(userId.toString(), "user@example.com");

        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(jwt, null);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Optional<CurrentUser> result = currentUserProvider.getCurrentUserOptional();

        // then
        assertTrue(result.isPresent());

        CurrentUser user = result.get();
        assertEquals(userId, user.userId());
        assertEquals("user@example.com", user.email());
        assertTrue(user.roles().isEmpty());
    }

    @Test
    void shouldThrowAuthenticationRequiredExceptionWhenNoUserPresent() {
        // given
        SecurityContextHolder.clearContext();

        // when + then
        assertThrows(
                AuthenticationRequiredException.class,
                () -> currentUserProvider.requireCurrentUser()
        );
    }

    @Test
    void shouldReturnUserFromRequireCurrentUserWhenPresent() {
        // given
        UUID userId = UUID.randomUUID();

        Jwt jwt = jwt(userId.toString(), "user@example.com", List.of("USER"));

        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(jwt, null);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        CurrentUser user = currentUserProvider.requireCurrentUser();

        // then
        assertNotNull(user);
        assertEquals(userId, user.userId());
        assertEquals("user@example.com", user.email());
        assertEquals(Set.of("USER"), user.roles());
    }

    private Jwt jwt(String subject, String email, List<String> roles) {
        return new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                java.util.Map.of("alg", "none"),
                java.util.Map.of(
                        "sub", subject,
                        Claims.EMAIL, email,
                        Claims.ROLES, roles
                )
        );
    }

    private Jwt jwtWithoutRoles(String subject, String email) {
        return new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                java.util.Map.of("alg", "none"),
                java.util.Map.of(
                        "sub", subject,
                        Claims.EMAIL, email
                )
        );
    }
}