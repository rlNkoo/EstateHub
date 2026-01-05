package com.rlnkoo.userservice.api.admin.dto;

import lombok.Builder;

import java.util.Set;
import java.util.UUID;

@Builder
public record ChangeRolesResponse(
        UUID userId,
        Set<String> roles,
        String message
) {
}