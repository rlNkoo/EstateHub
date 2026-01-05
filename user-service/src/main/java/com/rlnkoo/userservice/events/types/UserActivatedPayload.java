package com.rlnkoo.userservice.events.types;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record UserActivatedPayload(
        UUID userId,
        String email,
        Instant activatedAt
) {
}