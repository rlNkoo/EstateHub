package com.rlnkoo.commonsecurity;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthoritiesConverterTest {

    private final JwtAuthoritiesConverter converter = new JwtAuthoritiesConverter();

    @Test
    void shouldConvertRolesClaimToGrantedAuthorities() {
        Jwt jwt = jwtWithRoles(List.of("USER", "ADMIN"));

        // when
        Set<GrantedAuthority> authorities = converter.convert(jwt);

        // then
        assertEquals(2, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void shouldReturnEmptySetWhenRolesClaimIsNull() {
        Jwt jwt = jwtWithoutRoles();

        // when
        Set<GrantedAuthority> authorities = converter.convert(jwt);

        // then
        assertNotNull(authorities);
        assertTrue(authorities.isEmpty());
    }

    private Jwt jwtWithRoles(List<String> roles) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                java.util.Map.of("alg", "none"),
                java.util.Map.of(Claims.ROLES, roles)
        );
    }

    private Jwt jwtWithoutRoles() {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                java.util.Map.of("alg", "none"),
                java.util.Map.of("sub", "user-123")
        );
    }
}