package com.rlnkoo.userservice.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private final TokenService tokenService = new TokenService();

    @Test
    void shouldGenerateNonEmptyUrlSafeToken() {
        // when
        String token = tokenService.generateUrlSafeToken(32);

        // then
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void shouldGenerateDifferentTokensForSubsequentCalls() {
        // when
        String token1 = tokenService.generateUrlSafeToken(32);
        String token2 = tokenService.generateUrlSafeToken(32);

        // then
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }

    @Test
    void shouldGenerateUrlSafeTokenWithoutPlusSlashOrPadding() {
        // when
        String token = tokenService.generateUrlSafeToken(32);

        // then
        assertFalse(token.contains("+"));
        assertFalse(token.contains("/"));
        assertFalse(token.contains("="));
    }

    @Test
    void shouldReturnSameHashForSameInput() {
        // given
        String input = "my-test-token";

        // when
        String hash1 = tokenService.sha256Hex(input);
        String hash2 = tokenService.sha256Hex(input);

        // then
        assertEquals(hash1, hash2);
    }

    @Test
    void shouldReturn64CharacterHexSha256Hash() {
        // given
        String input = "my-test-token";

        // when
        String hash = tokenService.sha256Hex(input);

        // then
        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }
}