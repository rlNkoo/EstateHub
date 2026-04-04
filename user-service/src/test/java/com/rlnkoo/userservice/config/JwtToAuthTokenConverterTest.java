package com.rlnkoo.userservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class JwtToAuthTokenConverterTest {

    private final JwtToAuthTokenConverter converter = new JwtToAuthTokenConverter();

    @Test
    void shouldConvertJwtRolesToSpringAuthorities() {
        // given
        String subject = UUID.randomUUID().toString();

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(subject)
                .claim("roles", List.of("USER", "ADMIN"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // when
        AbstractAuthenticationToken authentication = converter.convert(jwt);

        // then
        assertNotNull(authentication);
        assertInstanceOf(JwtAuthenticationToken.class, authentication);
        assertEquals(subject, authentication.getName());
        assertSame(jwt, authentication.getPrincipal());

        Set<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertEquals(Set.of("ROLE_USER", "ROLE_ADMIN"), authorities);
    }

    @Test
    void shouldReturnEmptyAuthoritiesWhenRolesClaimIsMissing() {
        // given
        String subject = UUID.randomUUID().toString();

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // when
        AbstractAuthenticationToken authentication = converter.convert(jwt);

        // then
        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().isEmpty());
        assertEquals(subject, authentication.getName());
        assertSame(jwt, authentication.getPrincipal());
    }
}