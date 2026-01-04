package com.rlnkoo.userservice.events.types;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UserRegisteredPayload(
        UUID userId,
        String email,
        String activationToken
) {
}