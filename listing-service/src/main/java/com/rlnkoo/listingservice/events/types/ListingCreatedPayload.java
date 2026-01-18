package com.rlnkoo.listingservice.events.types;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ListingCreatedPayload(
        UUID listingId,
        UUID ownerId,
        String status,
        int version
) {
}