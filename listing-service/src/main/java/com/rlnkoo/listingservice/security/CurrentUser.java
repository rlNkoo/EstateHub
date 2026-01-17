package com.rlnkoo.listingservice.security;

import lombok.Builder;

import java.util.Set;
import java.util.UUID;

@Builder
public record CurrentUser(
        UUID userId,
        String email,
        Set<String> roles
) {
}