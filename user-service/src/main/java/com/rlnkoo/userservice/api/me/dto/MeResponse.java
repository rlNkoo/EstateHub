package com.rlnkoo.userservice.api.me.dto;

import lombok.Builder;

import java.util.Set;
import java.util.UUID;

@Builder
public record MeResponse(
        UUID userId,
        String email,
        Set<String> roles,
        boolean activated
) {
}