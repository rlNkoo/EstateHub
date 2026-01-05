package com.rlnkoo.userservice.api.auth.dto;

import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}
