package com.rlnkoo.mediaservice.api.internal.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record PhotoValidationError(
        UUID mediaId,
        String code,
        String message
) {
}