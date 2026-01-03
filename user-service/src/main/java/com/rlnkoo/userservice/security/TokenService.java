package com.rlnkoo.userservice.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class TokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateUrlSafeToken(int bytes) {
        byte[] buffer = new byte[bytes];
        SECURE_RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    public String sha256Hex(String tokenPlaintext) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tokenPlaintext.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash token with SHA-256", e);
        }
    }

    private String toHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        char[] alphabet = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = alphabet[v >>> 4];
            hex[i * 2 + 1] = alphabet[v & 0x0F];
        }
        return new String(hex);
    }
}