package com.rlnkoo.listingservice.events.types;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ListingArchivedPayload(
        UUID listingId,
        UUID ownerId,
        String status,
        int version,
        Instant archivedAt
) {
}