package com.rlnkoo.commonsecurity;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class JwtAuthenticationConverterFactory {
    private JwtAuthenticationConverterFactory() {}

    public static Converter<Jwt, AbstractAuthenticationToken> create() {
        JwtAuthoritiesConverter authoritiesConverter = new JwtAuthoritiesConverter();
        return jwt -> new JwtAuthenticationToken(jwt, authoritiesConverter.convert(jwt), jwt.getSubject());
    }
}