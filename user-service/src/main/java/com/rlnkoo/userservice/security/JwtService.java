package com.rlnkoo.userservice.security;

import com.rlnkoo.userservice.config.JwtProperties;
import com.rlnkoo.userservice.domain.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public String generateAccessToken(UUID userId, String email, Set<Role> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getExpiresMinutes(), ChronoUnit.MINUTES);

        var roleNames = roles.stream()
                .map(Role::name)
                .collect(Collectors.toSet());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roleNames)
                .build();

        JwsHeader header = JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS512).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
