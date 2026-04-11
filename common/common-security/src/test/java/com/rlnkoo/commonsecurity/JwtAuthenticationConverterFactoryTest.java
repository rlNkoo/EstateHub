package com.rlnkoo.commonsecurity;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationConverterFactoryTest {

    @Test
    void shouldCreateAuthenticationTokenWithSubjectAndAuthorities() {
        Converter<Jwt, AbstractAuthenticationToken> converter =
                JwtAuthenticationConverterFactory.create();

        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                java.util.Map.of("alg", "none"),
                java.util.Map.of(
                        "sub", "user-123",
                        Claims.ROLES, List.of("USER")
                )
        );

        // when
        AbstractAuthenticationToken auth = converter.convert(jwt);

        // then
        assertNotNull(auth);
        assertInstanceOf(JwtAuthenticationToken.class, auth);
        assertEquals("user-123", auth.getName());
        assertEquals(1, auth.getAuthorities().size());
    }
}