package com.rlnkoo.userservice.api.auth.dto;

import lombok.Builder;

@Builder
public record RegisterResponse(
        String message,
        boolean activationRequired
) {
}