package com.rlnkoo.listingservice.events.types;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record ListingPublishedPayload(
        UUID listingId,
        UUID ownerId,
        String status,
        int version,

        Instant publishedAt,

        String title,
        String description,

        BigDecimal priceAmount,
        String currencyCode,

        ListingUpdatedPayload.AddressPayload address,

        BigDecimal area,
        Integer rooms,
        Integer floor,
        String propertyType,

        List<UUID> photoIds
) {
}