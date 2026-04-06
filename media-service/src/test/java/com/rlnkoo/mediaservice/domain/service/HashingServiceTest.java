package com.rlnkoo.mediaservice.domain.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class HashingServiceTest {

    private final HashingService hashingService = new HashingService();

    @Test
    void shouldReturnCorrectSha256HexForGivenBytes() {
        // given
        byte[] bytes = "hello".getBytes();

        // when
        String result = hashingService.sha256Hex(bytes);

        // then
        assertEquals(
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                result
        );
    }

    @Test
    void shouldReturnSameHashForSameInput() {
        // given
        byte[] bytes = "same-input".getBytes();

        // when
        String hash1 = hashingService.sha256Hex(bytes);
        String hash2 = hashingService.sha256Hex(bytes);

        // then
        assertEquals(hash1, hash2);
    }

    @Test
    void shouldReturnDifferentHashForDifferentInput() {
        // given
        byte[] bytes1 = "input-one".getBytes();
        byte[] bytes2 = "input-two".getBytes();

        // when
        String hash1 = hashingService.sha256Hex(bytes1);
        String hash2 = hashingService.sha256Hex(bytes2);

        // then
        assertNotEquals(hash1, hash2);
    }

    @Test
    void shouldReturn64CharacterHexString() {
        // given
        byte[] bytes = "hello".getBytes();

        // when
        String result = hashingService.sha256Hex(bytes);

        // then
        assertEquals(64, result.length());
    }
}