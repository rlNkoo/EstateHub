package com.rlnkoo.userservice.security;

import com.rlnkoo.userservice.config.JwtProperties;
import com.rlnkoo.userservice.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private JwtService jwtService;

    @Test
    void shouldGenerateAccessTokenWithExpectedClaims() {
        // given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        Set<Role> roles = Set.of(Role.USER, Role.ADMIN);

        when(jwtProperties.getIssuer()).thenReturn("estatehub-user-service");
        when(jwtProperties.getExpiresMinutes()).thenReturn(60L);

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("encoded-jwt-token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        // when
        String token = jwtService.generateAccessToken(userId, email, roles);

        // then
        assertEquals("encoded-jwt-token", token);

        ArgumentCaptor<JwtEncoderParameters> parametersCaptor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        verify(jwtEncoder).encode(parametersCaptor.capture());

        JwtEncoderParameters parameters = parametersCaptor.getValue();
        assertNotNull(parameters);

        JwsHeader headers = parameters.getJwsHeader();
        assertNotNull(headers);
        assertEquals("HS256", headers.getAlgorithm().getName());

        JwtClaimsSet claims = parameters.getClaims();
        assertNotNull(claims);

        assertEquals("estatehub-user-service", claims.getClaim("iss"));
        assertEquals(userId.toString(), claims.getSubject());
        assertEquals(email, claims.getClaim("email"));

        Object rolesClaim = claims.getClaim("roles");
        assertNotNull(rolesClaim);
        assertTrue(rolesClaim instanceof Set<?> || rolesClaim instanceof List<?>);

        if (rolesClaim instanceof Set<?> roleSet) {
            assertEquals(Set.of("USER", "ADMIN"), roleSet);
        } else if (rolesClaim instanceof List<?> roleList) {
            assertEquals(Set.of("USER", "ADMIN"), Set.copyOf((List<String>) roleList));
        }

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiresAt());
        assertTrue(claims.getExpiresAt().isAfter(claims.getIssuedAt()));

        verify(jwtProperties).getIssuer();
        verify(jwtProperties).getExpiresMinutes();
    }

    @Test
    void shouldReturnEncodedTokenValue() {
        // given
        UUID userId = UUID.randomUUID();

        when(jwtProperties.getIssuer()).thenReturn("estatehub-user-service");
        when(jwtProperties.getExpiresMinutes()).thenReturn(30L);

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("my-token-value");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        // when
        String result = jwtService.generateAccessToken(
                userId,
                "user@example.com",
                Set.of(Role.USER)
        );

        // then
        assertEquals("my-token-value", result);
        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }
}