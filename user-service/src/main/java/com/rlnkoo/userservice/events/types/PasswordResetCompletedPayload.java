package com.rlnkoo.userservice.events.types;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record PasswordResetCompletedPayload(
        UUID userId,
        String email,
        Instant completedAt
) {
}