package com.rlnkoo.listingservice.api.listings.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ListingSummaryResponse(
        UUID id,
        String status,
        int version,
        Instant updatedAt
) {
}