package com.rlnkoo.userservice.events.types;

import lombok.Builder;

import java.util.UUID;

@Builder
public record PasswordResetRequestedPayload(
        UUID userId,
        String email,
        String resetToken
) {
}