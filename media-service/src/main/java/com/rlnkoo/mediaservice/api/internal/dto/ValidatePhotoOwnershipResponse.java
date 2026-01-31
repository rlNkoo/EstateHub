package com.rlnkoo.mediaservice.api.internal.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ValidatePhotoOwnershipResponse(
        boolean valid,
        List<PhotoValidationError> errors
) {
}