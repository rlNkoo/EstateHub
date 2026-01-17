package com.rlnkoo.commonsecurity;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JwtAuthoritiesConverter implements Converter<Jwt, Set<GrantedAuthority>> {

    @Override
    public Set<GrantedAuthority> convert(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(Claims.ROLES);
        if (roles == null) {
            roles = List.of();
        }

        return roles.stream()
                .map(Roles::asAuthority)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }
}