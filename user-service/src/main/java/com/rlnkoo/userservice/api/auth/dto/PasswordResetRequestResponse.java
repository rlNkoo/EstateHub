package com.rlnkoo.userservice.api.auth.dto;

import lombok.Builder;

@Builder
public record PasswordResetRequestResponse(
        String message
) {
}
